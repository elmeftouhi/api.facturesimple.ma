package com.elmeftouhi.facturesimple.provider.category;

import com.elmeftouhi.facturesimple.provider.category.dto.ProviderCategoryCreateRequest;
import com.elmeftouhi.facturesimple.provider.category.dto.ProviderCategoryResponse;
import com.elmeftouhi.facturesimple.provider.category.dto.ProviderCategoryUpdateRequest;
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
@RequestMapping("/v1/provider-categories")
@RequiredArgsConstructor
public class ProviderCategoryController {

    private final ProviderCategoryService providerCategoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProviderCategoryResponse create(@Valid @RequestBody ProviderCategoryCreateRequest request) {
        return providerCategoryService.create(request);
    }

    @GetMapping
    public List<ProviderCategoryResponse> findAll() {
        return providerCategoryService.findAll();
    }

    @GetMapping("/{id}")
    public ProviderCategoryResponse findById(@PathVariable Long id) {
        return providerCategoryService.findById(id);
    }

    @PutMapping("/{id}")
    public ProviderCategoryResponse update(@PathVariable Long id, @Valid @RequestBody ProviderCategoryUpdateRequest request) {
        return providerCategoryService.update(id, request);
    }

    @PutMapping("/{id}/default")
    public ProviderCategoryResponse setDefault(@PathVariable Long id) {
        return providerCategoryService.setDefault(id);
    }

    @DeleteMapping("/{id}/default")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsetDefault(@PathVariable Long id) {
        providerCategoryService.unsetDefault(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        providerCategoryService.delete(id);
    }
}

