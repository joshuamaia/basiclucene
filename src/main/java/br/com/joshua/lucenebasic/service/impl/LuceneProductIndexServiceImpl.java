package br.com.joshua.lucenebasic.service.impl;

import br.com.joshua.lucenebasic.model.Product;
import br.com.joshua.lucenebasic.model.dto.ProductResponse;
import br.com.joshua.lucenebasic.repository.ProductRepository;
import br.com.joshua.lucenebasic.service.LuceneProductIndexService;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class LuceneProductIndexServiceImpl implements LuceneProductIndexService {

    private final ProductRepository productRepository;

    @Value("${lucene.index.dir:C:/joshua/testes}")
    private String indexDir;

    private final Analyzer analyzer = new StandardAnalyzer();

    public LuceneProductIndexServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    private void clearIndexDirectory() throws IOException {
        Path indexPath = Paths.get(indexDir);

        if (Files.exists(indexPath)) {
            Files.walk(indexPath)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            System.out.println("Índice Lucene limpo com sucesso.");
        } else {
            System.out.println("Diretório de índices não existe.");
        }
    }

    private static String removeAccents(String text) {
        if (text == null) return null;
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", ""); // Remove acentos
    }


    public void indexProducts() throws IOException {
        clearIndexDirectory();
        try (FSDirectory dir = FSDirectory.open(Paths.get(indexDir));
             IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(analyzer))) {

            List<Product> products = productRepository.findAll();
            for (Product product : products) {
                Document doc = new Document();
                doc.add(new StringField("id", product.getId().toString(), Field.Store.YES));
                doc.add(new TextField("name", product.getName(), Field.Store.YES));
                doc.add(new TextField("description", product.getDescription(), Field.Store.YES));

                // Campo SEM tokenização para buscas exatas e parciais
                doc.add(new TextField("description_keyword", product.getDescription().toLowerCase(), Field.Store.YES));

                doc.add(new StringField("price", product.getPrice().toString(), Field.Store.YES));
                writer.addDocument(doc);
            }
        }
    }


    public List<ProductResponse> search(String queryString, boolean exactMatch) throws Exception {
        List<ProductResponse> results = new ArrayList<>();

        try (FSDirectory dir = FSDirectory.open(Paths.get(indexDir));
             DirectoryReader reader = DirectoryReader.open(dir)) {

            IndexSearcher searcher = new IndexSearcher(reader);
            Query query;

            // Normaliza a entrada do usuário removendo acentos
            String normalizedQuery = removeAccents(queryString.toLowerCase());

            if (exactMatch) {
                // Agora usa `description_keyword` com `QueryParser` para permitir `LIKE '%Teclado%'`
                QueryParser parser = new QueryParser("description_keyword", analyzer);
                query = parser.parse("\"" + queryString + "\" OR \"" + normalizedQuery + "\"");
            } else {
                // Busca flexível que aceita a palavra com e sem acento
                QueryParser parser = new QueryParser("description_keyword", analyzer);
                query = parser.parse(queryString + " OR " + normalizedQuery);
            }

            TopDocs topDocs = searcher.search(query, 10);
            for (ScoreDoc hit : topDocs.scoreDocs) {
                Document doc = searcher.doc(hit.doc);
                ProductResponse product = new ProductResponse();
                product.setId(Long.parseLong(doc.get("id")));
                product.setName(doc.get("name"));
                product.setDescription(doc.get("description"));
                product.setPrice(BigDecimal.valueOf(Double.parseDouble(doc.get("price"))));
                results.add(product);
            }
        }
        return results;
    }

}
