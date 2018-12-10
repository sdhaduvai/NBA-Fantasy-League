package com.example.demo;

import org.springframework.data.repository.CrudRepository;

import com.example.demo.FacebookDB;

public interface FacebookDBRepository extends CrudRepository<FacebookDB, String> {
}
