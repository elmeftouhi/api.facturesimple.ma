package com.elmeftouhi.facturesimple.provider;

import com.elmeftouhi.facturesimple.provider.dto.ProviderCreateRequest;
import com.elmeftouhi.facturesimple.provider.dto.ProviderResponse;
import com.elmeftouhi.facturesimple.provider.dto.ProviderUpdateRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/providers")
@RequiredArgsConstructor
public class ProviderController {

    private final ProviderService providerService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProviderResponse create(@Valid @RequestBody ProviderCreateRequest request) {
        return providerService.create(request);
    }

    @GetMapping
    public List<ProviderResponse> findAll() {
        return providerService.findAll();
    }

    @GetMapping("/{id}")
    public ProviderResponse findById(@PathVariable Long id) {
        return providerService.findById(id);
    }

    @PutMapping("/{id}")
    public ProviderResponse update(@PathVariable Long id, @Valid @RequestBody ProviderUpdateRequest request) {
        return providerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        providerService.delete(id);
    }
}

