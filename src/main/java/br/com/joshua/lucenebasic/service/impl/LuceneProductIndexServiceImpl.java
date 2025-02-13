package br.com.joshua.lucenebasic.service.impl;

import br.com.joshua.lucenebasic.model.Product;
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

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class LuceneProductIndexServiceImpl implements LuceneProductIndexService {

    private final ProductRepository productRepository;

    @Value("${lucene.index.dir:C:/joshua/testes}") // Diretório do índice configurável no application.yml
    private String indexDir;

    private final Analyzer analyzer = new StandardAnalyzer(); // Usando um analisador padrão do Lucene

    public LuceneProductIndexServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public void indexProducts() throws IOException {
        try (FSDirectory dir = FSDirectory.open(Paths.get(indexDir));
             IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(analyzer))) {

            List<Product> products = productRepository.findAll();
            for (Product product : products) {
                Document doc = new Document();
                doc.add(new StringField("id", product.getId().toString(), Field.Store.YES));
                doc.add(new TextField("name", product.getName(), Field.Store.YES));
                doc.add(new TextField("description", product.getDescription(), Field.Store.YES));

                // Novo campo SEM tokenização para buscas exatas
                doc.add(new StringField("description_keyword", product.getDescription().toLowerCase(), Field.Store.YES));

                doc.add(new StringField("price", product.getPrice().toString(), Field.Store.YES));
                writer.addDocument(doc);
            }
        }
    }

    public List<Product> search(String queryString, boolean exactMatch) throws Exception {
        List<Product> results = new ArrayList<>();

        try (FSDirectory dir = FSDirectory.open(Paths.get(indexDir));
             DirectoryReader reader = DirectoryReader.open(dir)) {

            IndexSearcher searcher = new IndexSearcher(reader);
            Query query;

            if (exactMatch) {
                query = new WildcardQuery(new Term("description_keyword", "*" + queryString.toLowerCase() + "*"));
            } else {
                QueryParser parser = new QueryParser("description", analyzer);
                query = parser.parse(queryString);
            }

            TopDocs topDocs = searcher.search(query, 10);
            for (ScoreDoc hit : topDocs.scoreDocs) {
                Document doc = searcher.doc(hit.doc);
                Product product = new Product();
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
