package com.shkgroups.provisioning;

import com.shkgroups.provisioning.dto.ProvisionPairingRequest;
import com.shkgroups.provisioning.dto.ProvisionPairingResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/provisioning")
public class ProvisioningController {

    private final ProvisioningService provisioningService;

    @PostMapping("/whatsapp/pairing")
    public ProvisionPairingResponse provision(@RequestBody @Valid ProvisionPairingRequest req) {
        return provisioningService.provisionPairing(req);
    }
}