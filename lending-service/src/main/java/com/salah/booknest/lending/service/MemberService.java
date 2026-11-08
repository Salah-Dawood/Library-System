package com.salah.booknest.lending.service;

import com.salah.booknest.lending.exception.InformationNotFoundException;
import com.salah.booknest.lending.model.Member;
import com.salah.booknest.lending.repository.MemberRepository;
import com.salah.booknest.lending.security.TokenUser;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The logged-in user as a lending member. Replaces reading the users table, which belongs to user-service.
 */
@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final TransactionTemplate newTransaction;

    public MemberService(MemberRepository memberRepository, PlatformTransactionManager transactionManager) {
        this.memberRepository = memberRepository;
        this.newTransaction = new TransactionTemplate(transactionManager);
        this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public static TokenUser tokenUser(Authentication authentication) {
        return (TokenUser) authentication.getPrincipal();
    }

    /**
     * Locks the member row of the logged-in user (creating it on first use).
     * Borrowing, claiming a hold and joining a waitlist all lock the member first, then the book.
     */
    public Member lockCurrentMember(Authentication authentication) {
        TokenUser user = tokenUser(authentication);
        addIfMissing(user);
        return memberRepository.findWithLockById(user.id())
                .orElseThrow(() -> new InformationNotFoundException("Member " + user.id() + " not found"));
    }

    /**
     * Saves the member in its own small transaction, so two first requests at the same moment cannot
     * break the caller's transaction: the second insert fails and is simply ignored.
     */
    private void addIfMissing(TokenUser user) {
        if (memberRepository.existsById(user.id())) {
            return;
        }
        try {
            newTransaction.executeWithoutResult(status -> memberRepository.saveAndFlush(new Member(user.id(), user.username())));
        } catch (DataIntegrityViolationException e) {
            // another request added the same member a moment ago
        }
    }
}
