package com.elmeftouhi.facturesimple.customer.category;

import com.elmeftouhi.facturesimple.customer.category.dto.CustomerCategoryCreateRequest;
import com.elmeftouhi.facturesimple.customer.category.dto.CustomerCategoryResponse;
import com.elmeftouhi.facturesimple.customer.category.dto.CustomerCategoryUpdateRequest;
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
@RequestMapping("/v1/customer-categories")
@RequiredArgsConstructor
public class CustomerCategoryController {

    private final CustomerCategoryService customerCategoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerCategoryResponse create(@Valid @RequestBody CustomerCategoryCreateRequest request) {
        return customerCategoryService.create(request);
    }

    @GetMapping
    public List<CustomerCategoryResponse> findAll() {
        return customerCategoryService.findAll();
    }

    @GetMapping("/{id}")
    public CustomerCategoryResponse findById(@PathVariable Long id) {
        return customerCategoryService.findById(id);
    }

    @PutMapping("/{id}")
    public CustomerCategoryResponse update(@PathVariable Long id, @Valid @RequestBody CustomerCategoryUpdateRequest request) {
        return customerCategoryService.update(id, request);
    }

    @PutMapping("/{id}/default")
    public CustomerCategoryResponse setDefault(@PathVariable Long id) {
        return customerCategoryService.setDefault(id);
    }

    @DeleteMapping("/{id}/default")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsetDefault(@PathVariable Long id) {
        customerCategoryService.unsetDefault(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        customerCategoryService.delete(id);
    }
}

