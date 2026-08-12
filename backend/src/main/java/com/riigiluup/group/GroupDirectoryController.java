package com.riigiluup.group;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public directory of parliamentary friendship groups, topic support groups and delegations.
 * {externalId} is the Riigikogu group UUID, matching the /committees/{externalId} scheme.
 */
@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GroupDirectoryController {

    private final GroupDirectoryService service;

    @GetMapping
    public List<GroupDirectoryDto.ListItem> list() {
        return service.list();
    }

    @GetMapping("/{externalId}")
    public ResponseEntity<GroupDirectoryDto.Detail> detail(@PathVariable String externalId) {
        return service.detail(externalId).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
