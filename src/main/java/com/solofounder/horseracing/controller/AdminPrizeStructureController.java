package com.solofounder.horseracing.controller;

import com.solofounder.horseracing.dto.prizestructure.PrizeStructureRequest;
import com.solofounder.horseracing.dto.prizestructure.PrizeStructureResponse;
import com.solofounder.horseracing.service.PrizeStructureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/prize-structures")
@RequiredArgsConstructor
public class AdminPrizeStructureController {

    private final PrizeStructureService prizeStructureService;

    @GetMapping
    public ResponseEntity<List<PrizeStructureResponse>> getAllPrizeStructures() {
        return ResponseEntity.ok(prizeStructureService.getAllPrizeStructures());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PrizeStructureResponse> getPrizeStructure(@PathVariable Long id) {
        return ResponseEntity.ok(prizeStructureService.getPrizeStructure(id));
    }

    @PostMapping
    public ResponseEntity<PrizeStructureResponse> createPrizeStructure(@Valid @RequestBody PrizeStructureRequest request) {
        return ResponseEntity.ok(prizeStructureService.createPrizeStructure(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PrizeStructureResponse> updatePrizeStructure(@PathVariable Long id,
                                                                       @Valid @RequestBody PrizeStructureRequest request) {
        return ResponseEntity.ok(prizeStructureService.updatePrizeStructure(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePrizeStructure(@PathVariable Long id) {
        prizeStructureService.deletePrizeStructure(id);
        return ResponseEntity.noContent().build();
    }
}
