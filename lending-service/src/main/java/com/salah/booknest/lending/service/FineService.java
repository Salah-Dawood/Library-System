package com.salah.booknest.lending.service;

import com.salah.booknest.lending.exception.InformationNotFoundException;
import com.salah.booknest.lending.exception.InvalidStateException;
import com.salah.booknest.lending.model.Fine;
import com.salah.booknest.lending.model.FineStatus;
import com.salah.booknest.lending.model.response.FineResponse;
import com.salah.booknest.lending.repository.FineRepository;
import com.salah.booknest.lending.util.PageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Service
public class FineService {

    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "createdAt", "amount", "status", "type");

    private final FineRepository fineRepository;

    public FineService(FineRepository fineRepository) {
        this.fineRepository = fineRepository;
    }

    public FineResponse fineResponser(Fine fine){
        FineResponse response = new FineResponse();
        response.setId(fine.getId());
        response.setLoanId(fine.getLoan().getId());
        response.setType(fine.getType().toString());
        response.setStatus(fine.getStatus().toString());
        response.setAmount(fine.getAmount());
        response.setReason(fine.getReason());

        return response;
    }



    public Page<FineResponse> getAllFines(int page, int size, String sortBy, String sortDir){
        Pageable pageable = PageUtils.pageable(page, size, sortBy, sortDir, SORTABLE_FIELDS);
        return fineRepository.findAll(pageable).map(this::fineResponser);
    }

    public Page<FineResponse> getMyFines(Authentication authentication, int page, int size, String sortBy, String sortDir){
        return getUserFines(MemberService.tokenUser(authentication).id(), page, size, sortBy, sortDir);
    }

    public Page<FineResponse> getUserFines(Long id, int page, int size, String sortBy, String sortDir){
        Pageable pageable = PageUtils.pageable(page, size, sortBy, sortDir, SORTABLE_FIELDS);
        return fineRepository.findAllByLoanUserId(id, pageable).map(this::fineResponser);
    }

    @Transactional
    public FineResponse payFine(Long id, Authentication authentication){
        // the fine row is locked, so two payments of the same fine cannot both go through
        Fine fine = fineRepository.findWithLockById(id).orElseThrow(() -> new InformationNotFoundException("Fine with ID " + id + " not found!"));
        if (!fine.getLoan().getUsername().equals(authentication.getName())) {
            throw new AccessDeniedException("You can only pay your own fines");
        }
        if (fine.getStatus() == FineStatus.PAID) {
            throw new InvalidStateException("This fine is already paid", "FINE_ALREADY_PAID");
        }

        fine.setStatus(FineStatus.PAID);
        log.info("Fine {} paid by {}", id, authentication.getName());
        return fineResponser(fine);
    }
}
