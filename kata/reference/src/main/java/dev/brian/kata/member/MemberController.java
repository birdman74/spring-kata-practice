package dev.brian.kata.member;

import dev.brian.kata.loan.LoanResponse;
import dev.brian.kata.loan.LoanService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;
    private final LoanService loanService;

    public MemberController(MemberService memberService, LoanService loanService) {
        this.memberService = memberService;
        this.loanService = loanService;
    }

    @PostMapping
    public ResponseEntity<MemberResponse> create(@Valid @RequestBody MemberRequest memberRequest) {
        MemberResponse createdMember = memberService.create(memberRequest);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdMember.id())
                .toUri();
        return ResponseEntity.created(location).body(createdMember);
    }

    @GetMapping("/{id}")
    public MemberResponse findById(@PathVariable("id") long memberId) {
        return memberService.findById(memberId);
    }

    @GetMapping("/{id}/loans")
    public List<LoanResponse> loans(@PathVariable("id") long memberId,
                                    @RequestParam(name = "active", defaultValue = "false") boolean activeOnly) {
        return loanService.loansOfMember(memberId, activeOnly);
    }
}
