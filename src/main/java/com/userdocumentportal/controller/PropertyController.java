package com.userdocumentportal.controller;

import com.userdocumentportal.entity.Property;
import com.userdocumentportal.repository.PropertyRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/properties")
public class PropertyController {

    private static final Logger logger = LoggerFactory.getLogger(PropertyController.class);

    @Autowired
    PropertyRepository propertyRepository;

    @GetMapping
    public List<Property> getAllProperties() {
        logger.info("Received request to retrieve all properties");
        List<Property> properties = propertyRepository.findAll();
        logger.info("Returning {} properties in response", properties.size());
        return properties;
    }

    @PostMapping
    public Property createProperty(@RequestBody Property property) {
        logger.info("Received request to create new property titled: '{}'", property.getTitle());
        Property savedProperty = propertyRepository.save(property);
        logger.info("Successfully created property ID: {} titled: '{}'", savedProperty.getId(), savedProperty.getTitle());
        return savedProperty;
    }

    @PutMapping("/{id}")
    public ResponseEntity<Property> updateProperty(@PathVariable Long id, @RequestBody Property propertyDetails) {
        logger.info("Received request to update property ID: {}", id);
        
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Property update failed: Property not found with id: {}", id);
                    return new RuntimeException("Property not found with id: " + id);
                });

        property.setTitle(propertyDetails.getTitle());
        property.setType(propertyDetails.getType());
        property.setRent(propertyDetails.getRent());
        property.setAddress(propertyDetails.getAddress());
        property.setBeds(propertyDetails.getBeds());
        property.setBaths(propertyDetails.getBaths());
        property.setStatus(propertyDetails.getStatus());

        Property updatedProperty = propertyRepository.save(property);
        logger.info("Successfully updated details for property ID: {}", id);
        return ResponseEntity.ok(updatedProperty);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProperty(@PathVariable Long id) {
        logger.info("Received request to delete property ID: {}", id);
        
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Property deletion failed: Property not found with id: {}", id);
                    return new RuntimeException("Property not found with id: " + id);
                });
        
        propertyRepository.delete(property);
        logger.info("Successfully deleted property ID: {}", id);
        return ResponseEntity.ok().build();
    }
}
