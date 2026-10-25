package com.deepbank.transfer.controller;

import com.deepbank.common.dto.ApiResponse;
import com.deepbank.common.dto.TransferRequest;
import com.deepbank.common.dto.TransferResponse;
import com.deepbank.transfer.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TransferResponse>> initiateTransfer(
            @Valid @RequestBody TransferRequest request) {
        TransferResponse response = transferService.initiateTransfer(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.ok("Transfer accepted for real-time FAST processing", response));
    }

    @GetMapping("/{transferId}")
    public ResponseEntity<ApiResponse<TransferResponse>> getTransfer(@PathVariable String transferId) {
        TransferResponse response = transferService.getTransfer(transferId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
