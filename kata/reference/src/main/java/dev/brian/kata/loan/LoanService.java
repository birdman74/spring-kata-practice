package dev.brian.kata.loan;

import dev.brian.kata.book.Book;
import dev.brian.kata.book.BookRepository;
import dev.brian.kata.error.ConflictException;
import dev.brian.kata.error.NotFoundException;
import dev.brian.kata.member.Member;
import dev.brian.kata.member.MemberRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class LoanService {

    static final int MAX_ACTIVE_LOANS_PER_MEMBER = 3;

    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;

    public LoanService(LoanRepository loanRepository, BookRepository bookRepository,
                       MemberRepository memberRepository) {
        this.loanRepository = loanRepository;
        this.bookRepository = bookRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional
    public LoanResponse create(LoanRequest loanRequest) {
        Book book = bookRepository.findById(loanRequest.bookId())
                .orElseThrow(() -> new NotFoundException("Book " + loanRequest.bookId() + " not found"));
        Member member = loadMember(loanRequest.memberId());
        if (loanRepository.existsByBookAndReturnedAtIsNull(book)) {
            throw new ConflictException("Book " + book.getId() + " is already on loan");
        }
        if (loanRepository.countByMemberAndReturnedAtIsNull(member) >= MAX_ACTIVE_LOANS_PER_MEMBER) {
            throw new ConflictException("Member " + member.getId() + " already has "
                    + MAX_ACTIVE_LOANS_PER_MEMBER + " active loans");
        }
        return LoanResponse.from(loanRepository.save(new Loan(book, member, Instant.now())));
    }

    @Transactional
    public LoanResponse returnLoan(long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new NotFoundException("Loan " + loanId + " not found"));
        if (!loan.isActive()) {
            throw new ConflictException("Loan " + loanId + " was already returned");
        }
        loan.markReturned(Instant.now());
        return LoanResponse.from(loan);
    }

    public List<LoanResponse> loansOfMember(long memberId, boolean activeOnly) {
        Member member = loadMember(memberId);
        List<Loan> loans = activeOnly
                ? loanRepository.findByMemberAndReturnedAtIsNullOrderByIdAsc(member)
                : loanRepository.findByMemberOrderByIdAsc(member);
        return loans.stream().map(LoanResponse::from).toList();
    }

    private Member loadMember(long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("Member " + memberId + " not found"));
    }
}
