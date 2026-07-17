package com.riigiluup.committee;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public committee API. Only the active standing committees are exposed; the {externalId}
 * is the Riigikogu group UUID, matching the /legislation/{uuid} and /votes/{uuid} scheme.
 */
@RestController
@RequestMapping("/api/v1/committees")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CommitteeController {

    private final CommitteeService service;

    @GetMapping
    public List<CommitteeDto.ListItem> list() {
        return service.list();
    }

    @GetMapping("/{externalId}")
    public ResponseEntity<CommitteeDto.Detail> detail(@PathVariable String externalId) {
        return service.detail(externalId).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
