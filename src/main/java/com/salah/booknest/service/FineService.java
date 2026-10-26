package com.salah.booknest.service;

import com.salah.booknest.exception.InformationNotFoundException;
import com.salah.booknest.exception.InvalidStateException;
import com.salah.booknest.model.Fine;
import com.salah.booknest.model.FineStatus;
import com.salah.booknest.model.response.FineResponse;
import com.salah.booknest.repository.FineRepository;
import com.salah.booknest.repository.LoanRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FineService {

    private final FineRepository fineRepository;
    private final LoanRepository loanRepository;
    private final AppSharedService appSharedService;
    private final LoanService loanService;

    public FineService(FineRepository fineRepository, LoanRepository loanRepository, AppSharedService appSharedService, LoanService loanService) {
        this.fineRepository = fineRepository;
        this.loanRepository = loanRepository;
        this.appSharedService = appSharedService;
        this.loanService = loanService;
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



    public List<FineResponse> getAllFines(){
        return fineRepository.findAll().stream()
                .map(this::fineResponser)
                .toList();
    }

    public List<FineResponse> getMyFines(Authentication authentication){
        return loanRepository.findAllByUserId(appSharedService.getUserFromAuth(authentication).getId(), Sort.unsorted())
                .stream().flatMap(loan -> loan.getFines().stream())
                .map(this::fineResponser)
                .toList();
    }

    public List<FineResponse> getUserFines(Long id){
        return loanRepository.findAllByUserId(id, Sort.unsorted())
                .stream().flatMap(loan -> loan.getFines().stream())
                .map(this::fineResponser)
                .toList();
    }

    @Transactional
    public FineResponse payFine(Long id, Authentication authentication){
        // the fine row is locked, so two payments of the same fine cannot both go through
        Fine fine = fineRepository.findWithLockById(id).orElseThrow(() -> new InformationNotFoundException("Fine with ID " + id + " not found!"));
        if (!fine.getLoan().getUser().getUsername().equals(authentication.getName())) {
            throw new AccessDeniedException("You can only pay your own fines");
        }
        if (fine.getStatus() == FineStatus.PAID) {
            throw new InvalidStateException("This fine is already paid", "FINE_ALREADY_PAID");
        }

        fine.setStatus(FineStatus.PAID);
        return fineResponser(fine);
    }
}
