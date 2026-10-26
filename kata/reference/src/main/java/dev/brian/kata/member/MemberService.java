package dev.brian.kata.member;

import dev.brian.kata.error.ConflictException;
import dev.brian.kata.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional
    public MemberResponse create(MemberRequest memberRequest) {
        if (memberRepository.existsByEmailIgnoreCase(memberRequest.email())) {
            throw new ConflictException("Member with email " + memberRequest.email() + " already exists");
        }
        return MemberResponse.from(memberRepository.save(
                new Member(memberRequest.name(), memberRequest.email())));
    }

    public MemberResponse findById(long memberId) {
        return memberRepository.findById(memberId)
                .map(MemberResponse::from)
                .orElseThrow(() -> new NotFoundException("Member " + memberId + " not found"));
    }
}
