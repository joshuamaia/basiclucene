package br.com.joshua.lucenebasic.repository;

import br.com.joshua.lucenebasic.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}

