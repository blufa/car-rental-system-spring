package com.example.carrentalsystem.Controllers;

import com.example.carrentalsystem.Models.*;
import com.example.carrentalsystem.Payload.Request.*;
import com.example.carrentalsystem.Payload.Response.MessageResponse;
import com.example.carrentalsystem.Repositories.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.temporal.ChronoUnit;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/rental")
public class RentalController {
    private final CarRepository carRepository;
    private final UserRepository userRepository;
    private final RentalRepository rentalRepository;
    private final RentalStatusRepository rentalStatusRepository;
    private final RoleRepository roleRepository;

    public RentalController(CarRepository carRepository, UserRepository userRepository, RentalRepository rentalRepository,
                            RentalStatusRepository rentalStatusRepository, RoleRepository roleRepository) {
        this.carRepository = carRepository;
        this.userRepository = userRepository;
        this.rentalRepository = rentalRepository;
        this.rentalStatusRepository = rentalStatusRepository;
        this.roleRepository = roleRepository;
    }

    @PostMapping("add")
    public ResponseEntity<?> addRental(@Valid @RequestBody AddCarRentalRequest addCarRentalRequest){
        if(userRepository.existsByToken(addCarRentalRequest.getToken())) {
            Car car = carRepository.getCarById(addCarRentalRequest.getCarID());

            rentalRepository.save(new Rental(
                    car,
                    userRepository.getUserByToken(addCarRentalRequest.getToken()),
                    addCarRentalRequest.getStartDate(),
                    addCarRentalRequest.getEndDate(),
                    addCarRentalRequest.getAddDate(),
                    Math.toIntExact((ChronoUnit.DAYS.between(addCarRentalRequest.getStartDate(), addCarRentalRequest.getEndDate())+1) * car.getPrice()),
                    rentalStatusRepository.findByName(ERentalStatus.STATUS_PENDING)
            ));

            return ResponseEntity.ok(new MessageResponse("Success: Successfully added rental!"));
        } else {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Bad token!"));
        }
    }

    @GetMapping("get/all")
    public ResponseEntity<?> getAllRentals(@Valid @RequestBody String token){
        if(userRepository.getByToken(token).getRoles().contains(roleRepository.getByName(ERole.ROLE_ADMIN))){
            if(rentalRepository.findAll().size() > 0){
                return ResponseEntity.ok(rentalRepository.findAll());
            }

            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: No rental found!"));
        } else {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Bad token!"));
        }
    }

    @GetMapping("get/user/{id}")
    public ResponseEntity<?> getUserRentals(@Valid @RequestBody String token, @PathVariable("id") Long id){
        if(userRepository.existsByToken(token)) {
            if(userRepository.existsById(id)){
                if(rentalRepository.findByUser_Id(id).size() > 0){
                    return ResponseEntity.ok(rentalRepository.findByUser_Id(id));
                }

                return ResponseEntity
                        .badRequest()
                        .body(new MessageResponse("Error: No rental found!"));
            }else{
                return ResponseEntity
                        .badRequest()
                        .body(new MessageResponse("Error: Bad user id!"));
            }
        } else {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Bad token!"));
        }
    }

    @GetMapping("get/{id}")
    public ResponseEntity<?> getRentalInfo(@Valid @RequestBody String token, @PathVariable("id") Long id){
        if(userRepository.existsByToken(token)) {
            if(rentalRepository.existsById(id)){
                return ResponseEntity.ok(rentalRepository.findById(id));
            }

            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: No rental found!"));
        } else {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Bad token!"));
        }
    }

    @PostMapping("status/{statusID}/rental/{id}")
    public ResponseEntity<?> changeStatus(@Valid @RequestBody String token, @PathVariable("statusID") Long statusID, @PathVariable("id") Long id){
        if(userRepository.existsByToken(token)) {
            if(rentalRepository.existsById(id)){
                if(rentalStatusRepository.existsById(statusID)){
                    Rental rental = rentalRepository.getReferenceById(id);
                    rental.setRentalStatus(rentalStatusRepository.getReferenceById(statusID));
                    rentalRepository.save(rental);

                    return ResponseEntity.ok(new MessageResponse("Success: Rent status changed successfully!"));
                }

                return ResponseEntity
                        .badRequest()
                        .body(new MessageResponse("Error: No rental status found!"));
            }

            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: No rental found!"));
        } else {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Bad token!"));
        }
    }

    @PostMapping("edit")
    public ResponseEntity<?> changeRentalInformation(@Valid @RequestBody EditCarRentalRequest editCarRentalRequest){
        if(userRepository.existsByToken(editCarRentalRequest.getToken())) {
            if(rentalRepository.existsById(editCarRentalRequest.getRentId())){
                Rental rental = rentalRepository.getReferenceById(editCarRentalRequest.getRentId());
                rental.setPrice(Math.toIntExact((ChronoUnit.DAYS.between(editCarRentalRequest.getStartDate(), editCarRentalRequest.getEndDate())+1) * rental.getCar().getPrice()));
                rental.setStartDate(editCarRentalRequest.getStartDate());
                rental.setEndDate(editCarRentalRequest.getEndDate());
                rentalRepository.save(rental);

                return ResponseEntity.ok(new MessageResponse("Success: Rent info changed successfully!"));
            }

            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: No rental found!"));
        } else {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Bad token!"));
        }
    }
}
