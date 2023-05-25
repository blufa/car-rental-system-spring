package com.example.carrentalsystem.Repositories;

import com.example.carrentalsystem.Models.Car;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CarRepository extends JpaRepository<Car, Long> {
    List<Car> findByAvailable(boolean available);

    Car getCarById(Long carID);

    List<Car> findByMileage(Integer mileage);

    List<Car> findByModel_Name(String name);

    List<Car> findByBrand_Name(String name);

}
