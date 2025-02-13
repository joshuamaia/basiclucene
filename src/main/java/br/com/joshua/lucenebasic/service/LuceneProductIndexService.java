package br.com.joshua.lucenebasic.service;

import br.com.joshua.lucenebasic.model.Product;

import java.io.IOException;
import java.util.List;

public interface LuceneProductIndexService {
    void indexProducts() throws IOException ;
    List<Product> search(String queryString, boolean exactMatch) throws Exception;
}
