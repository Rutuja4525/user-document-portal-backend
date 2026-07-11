package com.userdocumentportal.controller;

import com.userdocumentportal.entity.Property;
import com.userdocumentportal.repository.PropertyRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/properties")
public class PropertyController {

    @Autowired
    PropertyRepository propertyRepository;

    @GetMapping
    public List<Property> getAllProperties() {
        return propertyRepository.findAll();
    }

    @PostMapping
    public Property createProperty(@RequestBody Property property) {
        return propertyRepository.save(property);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Property> updateProperty(@PathVariable Long id, @RequestBody Property propertyDetails) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Property not found with id: " + id));

        property.setTitle(propertyDetails.getTitle());
        property.setType(propertyDetails.getType());
        property.setRent(propertyDetails.getRent());
        property.setAddress(propertyDetails.getAddress());
        property.setBeds(propertyDetails.getBeds());
        property.setBaths(propertyDetails.getBaths());
        property.setStatus(propertyDetails.getStatus());

        return ResponseEntity.ok(propertyRepository.save(property));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProperty(@PathVariable Long id) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Property not found with id: " + id));
        propertyRepository.delete(property);
        return ResponseEntity.ok().build();
    }
}
