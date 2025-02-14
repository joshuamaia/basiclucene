package br.com.joshua.lucenebasic.service;

import br.com.joshua.lucenebasic.model.dto.ProductResponse;

import java.io.IOException;
import java.util.List;

public interface LuceneProductIndexService {
    void indexProducts() throws IOException ;
    List<ProductResponse> search(String queryString, boolean exactMatch) throws Exception;
}
