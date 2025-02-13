package br.com.joshua.lucenebasic.controller;

import br.com.joshua.lucenebasic.model.Product;
import br.com.joshua.lucenebasic.service.LuceneProductIndexService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products/search")
public class ProductSearchController {

    private final LuceneProductIndexService luceneService;

    public ProductSearchController(LuceneProductIndexService luceneService) {
        this.luceneService = luceneService;
    }

    @PostMapping("/index")
    public String indexProducts() {
        try {
            luceneService.indexProducts();
            return "Indexação concluída com sucesso!";
        } catch (Exception e) {
            return "Erro na indexação: " + e.getMessage();
        }
    }

    @GetMapping
    public ResponseEntity<?> search(@RequestParam String query,
                                    @RequestParam(required = false, defaultValue = "false") boolean exactMatch) {
        try {
            List<Product> results = luceneService.search(query, exactMatch);
            if (results.isEmpty()) {
                return ResponseEntity.noContent().build(); // Retorna 204 se não encontrar resultados
            }
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erro ao realizar a busca: " + e.getMessage());
        }
    }
}

