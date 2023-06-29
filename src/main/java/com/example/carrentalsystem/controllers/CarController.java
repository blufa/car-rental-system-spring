package com.example.carrentalsystem.controllers;

import com.example.carrentalsystem.models.*;
import com.example.carrentalsystem.payload.request.*;
import com.example.carrentalsystem.repositories.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.io.IOException;
import java.time.LocalDate;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/cars")
public class CarController {
    private final CarRepository carRepository;
    private final BrandRepository brandRepository;
    private final CarModelRepository carModelRepository;
    private final FuelTypeRepository fuelTypeRepository;
    private final CarImageRepository carImageRepository;
    private final RentalRepository rentalRepository;

    public CarController(CarRepository carRepository, BrandRepository brandRepository, CarModelRepository carModelRepository,
                         FuelTypeRepository fuelTypeRepository, CarImageRepository carImageRepository, RentalRepository rentalRepository) {
        this.carRepository = carRepository;
        this.brandRepository = brandRepository;
        this.carModelRepository = carModelRepository;
        this.fuelTypeRepository = fuelTypeRepository;
        this.carImageRepository = carImageRepository;
        this.rentalRepository = rentalRepository;
    }

    @GetMapping("available")
    public ResponseEntity<?> getAvailableCars(){
        return ResponseEntity.ok(carRepository.findByAvailable(true));
    }

    @GetMapping("all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllCars(){
        return ResponseEntity.ok(carRepository.findAll());
    }

    @Transactional
    @PostMapping("add")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addCar(@RequestBody @Valid AddCarRequest carRequest){
        Brand brand = brandRepository.findByName(carRequest.getBrand());
        if (brand == null) {
            brand = brandRepository.save(new Brand(carRequest.getBrand()));
        }

        CarModel model = carModelRepository.findByName(carRequest.getModel());
        if (model == null) {
            model = carModelRepository.save(new CarModel(carRequest.getModel()));
        }

        FuelType fuelType = fuelTypeRepository.findById(carRequest.getFuelType())
                .orElseThrow(() -> new RuntimeException("Error: Fuel type is not found."));

        carRepository.save(new Car(
                brand,
                model,
                carRequest.getYear(),
                carRequest.getMileage(),
                fuelType,
                carRequest.getHorsePower(),
                carRequest.getCapacity(),
                carRequest.getPrice(),
                true,
                carImageRepository.getByImageID(1L)
            ));

        return ResponseEntity.ok(carRepository.findByAvailable(true));
    }

    @Transactional
    @PostMapping("change-image")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> changeCarImage(@RequestParam("myFile") MultipartFile file,
                                            @RequestParam("carID") Long carID) throws IOException {
        if(carRepository.existsById(carID)){
            Car car = carRepository.getCarById(carID);
            Long imageID = car.getCarImage().getImageID();

            CarImage carImage = carImageRepository.save(new CarImage(file.getBytes()));
            car.setCarImage(carImage);

            // CarImage with ID = 1 is the default image for new cars. For this reason, it must be protected from deletion
            if(imageID != 1) {
                carImageRepository.deleteById(imageID);
            }
            carRepository.save(car);

            return new ResponseEntity<>("Car photo changed", HttpStatus.OK);
        }

        return new ResponseEntity<>("Car not found", HttpStatus.NOT_FOUND);
    }

    @Transactional
    @PostMapping("edit")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> editCar(@RequestBody @Valid EditCarRequest carRequest){
        if(carRepository.existsById(carRequest.getId())){
            Car car = carRepository.getCarById(carRequest.getId());

            if(!carRequest.getBrand().equals(car.getBrand().getName())){
                Brand brand = car.getBrand();

                if(!brandRepository.existsByName(carRequest.getBrand())){
                    car.setBrand(brandRepository.save(new Brand(carRequest.getBrand())));
                } else {
                    car.setBrand(brandRepository.findByName(carRequest.getBrand()));
                }

                if(!carRepository.existsByBrandName(brand.getName())){
                    brandRepository.deleteByName(brand.getName());
                }
            }

            if(!carRequest.getModel().equals(car.getModel().getName())){
                CarModel carModel = car.getModel();

                if(!carModelRepository.existsByName(carRequest.getModel())){
                    car.setModel(carModelRepository.save(new CarModel(carRequest.getModel())));
                } else {
                    car.setModel(carModelRepository.findByName(carRequest.getModel()));
                }

                if(!carRepository.existsByModelName(carModel.getName())){
                    carModelRepository.deleteByName(carModel.getName());
                }
            }

            if(!carRequest.getCapacity().equals(car.getCapacity())){
                car.setCapacity(carRequest.getCapacity());
            }

            if(!carRequest.getHorsePower().equals(car.getHorsePower())){
                car.setHorsePower(carRequest.getHorsePower());
            }

            if(!carRequest.getYear().equals(car.getYear())){
                car.setYear(carRequest.getYear());
            }

            if(!carRequest.getMileage().equals(car.getMileage())){
                car.setMileage(carRequest.getMileage());
            }

            if(!carRequest.getPrice().equals(car.getPrice())){
                car.setPrice(carRequest.getPrice());
            }

            if(!carRequest.getFuelType().equals(car.getFuelType().getId())){
                car.setFuelType(fuelTypeRepository.findById(carRequest.getFuelType())
                        .orElseThrow(() -> new RuntimeException("Error: Fuel type is not found.")));
            }

            carRepository.save(car);
            return new ResponseEntity<>("Car information changed", HttpStatus.OK);
        }

        return new ResponseEntity<>("Car not found", HttpStatus.NOT_FOUND);
    }

    @GetMapping("get/{carID}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getCar(@PathVariable("carID") Long carID){
        if(carRepository.existsById(carID)){
            return ResponseEntity.ok(carRepository.getCarById(carID));
        }

        return new ResponseEntity<>("Car not found", HttpStatus.NOT_FOUND);
    }

    @PostMapping("status/{carID}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> changeCarStatus(@PathVariable("carID") Long carID){
        if(carRepository.existsById(carID)){
            Car car = carRepository.getCarById(carID);

            if(!rentalRepository.existsByRentalDateAndRentalStatus(LocalDate.now(), RentalStatusEnum.STATUS_ACCEPTED)){
                car.setAvailable(!car.isAvailable());
                carRepository.save(car);
                return new ResponseEntity<>("The availability of the car has been changed", HttpStatus.OK);
            }

            return new ResponseEntity<>("Car has active rental", HttpStatus.CONFLICT);
        }

        return new ResponseEntity<>("Car not found", HttpStatus.NOT_FOUND);
    }

    @Transactional
    @DeleteMapping("delete/{carID}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCar(@PathVariable("carID") Long carID){
        if(carRepository.existsById(carID)){
            Car car = carRepository.getCarById(carID);

            if(!rentalRepository.existsByCarId(car.getId())){
                if(carRepository.countByModelName(car.getModel().getName()) == 1){
                    carModelRepository.deleteById(car.getModel().getId());
                }

                if(carRepository.countByBrandName(car.getBrand().getName()) == 1){
                    brandRepository.deleteById(car.getBrand().getId());
                }

                // CarImage with ID = 1 is the default image for new cars. For this reason, it must be protected from deletion
                if(car.getCarImage().getImageID() != 1){
                    carImageRepository.deleteById(car.getCarImage().getImageID());
                }

                carRepository.deleteById(car.getId());
                return new ResponseEntity<>("Car removed successfully", HttpStatus.OK);
            }

            return new ResponseEntity<>("You cannot remove a car if it has a rental assigned to it", HttpStatus.CONFLICT);
        }

        return new ResponseEntity<>("Car not found", HttpStatus.NOT_FOUND);
    }

}
