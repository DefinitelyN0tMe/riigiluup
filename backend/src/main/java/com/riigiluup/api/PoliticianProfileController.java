package com.riigiluup.api;

import com.riigiluup.group.GroupMembershipRepository;
import com.riigiluup.person.PlenaryMember;
import com.riigiluup.person.PlenaryMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/politicians")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PoliticianProfileController {

    private final PlenaryMemberRepository memberRepo;
    private final GroupMembershipRepository membershipRepo;
    private final PoliticianProfileMapper mapper;

    @GetMapping("/{slug}")
    @Transactional(readOnly = true)
    public ResponseEntity<PoliticianProfileDto> get(@PathVariable String slug) {
        PlenaryMember m = memberRepo.findBySlug(slug).orElse(null);
        if (m == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(mapper.toDto(
                m, membershipRepo.findByPlenaryMemberAndActiveTrueWithGroupFetch(m)));
    }
}
