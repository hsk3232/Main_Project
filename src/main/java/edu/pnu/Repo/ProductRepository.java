package edu.pnu.Repo;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.pnu.domain.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

}
