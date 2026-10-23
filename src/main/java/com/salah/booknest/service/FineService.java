package com.salah.booknest.service;

import com.salah.booknest.exception.InformationNotFoundException;
import com.salah.booknest.model.Fine;
import com.salah.booknest.model.FineStatus;
import com.salah.booknest.model.response.FineResponse;
import com.salah.booknest.repository.FineRepository;
import com.salah.booknest.repository.LoanRepository;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

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

    public FineResponse payFine(Long id){
        Fine fine = fineRepository.findById(id).orElseThrow(() -> new InformationNotFoundException("Fine with ID " + id + " not found!"));
        fine.setStatus(FineStatus.PAID);

        return fineResponser(fine);
    }
}
