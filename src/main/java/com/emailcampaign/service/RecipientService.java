package com.emailcampaign.service;

import com.emailcampaign.dto.RecipientDto;
import com.emailcampaign.dto.RecipientListDto;
import com.emailcampaign.model.Recipient;
import com.emailcampaign.model.RecipientList;
import com.emailcampaign.model.User;
import com.emailcampaign.repository.RecipientListRepository;
import com.emailcampaign.repository.RecipientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RecipientService {
    
    private final RecipientListRepository recipientListRepository;
    private final RecipientRepository recipientRepository;
    private final SystemLogService systemLogService;
    
    // Recipient List Management
    public RecipientList createRecipientList(RecipientListDto dto, User user) {
        RecipientList recipientList = new RecipientList();
        recipientList.setName(dto.getName());
        recipientList.setDescription(dto.getDescription());
        recipientList.setUser(user);
        
        RecipientList savedList = recipientListRepository.save(recipientList);
        
        systemLogService.logUserAction(user, "RECIPIENT_LIST_CREATED", 
                "Recipient list created: " + savedList.getName(), null, null);
        
        log.info("Recipient list created: {} by user: {}", savedList.getName(), user.getUsername());
        return savedList;
    }
    
    public RecipientList updateRecipientList(Long id, RecipientListDto dto, User user) {
        RecipientList recipientList = recipientListRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recipient list not found"));
        
        if (!recipientList.getUser().getId().equals(user.getId()) && user.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("Access denied");
        }
        
        recipientList.setName(dto.getName());
        recipientList.setDescription(dto.getDescription());
        
        RecipientList savedList = recipientListRepository.save(recipientList);
        
        systemLogService.logUserAction(user, "RECIPIENT_LIST_UPDATED", 
                "Recipient list updated: " + savedList.getName(), null, null);
        
        return savedList;
    }
    
    public void deleteRecipientList(Long id, User user) {
        RecipientList recipientList = recipientListRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recipient list not found"));
        
        if (!recipientList.getUser().getId().equals(user.getId()) && user.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("Access denied");
        }
        
        recipientListRepository.delete(recipientList);
        
        systemLogService.logUserAction(user, "RECIPIENT_LIST_DELETED", 
                "Recipient list deleted: " + recipientList.getName(), null, null);
    }
    
    public Page<RecipientList> getUserRecipientLists(User user, Pageable pageable) {
        return recipientListRepository.findByUser(user, pageable);
    }
    
    public Optional<RecipientList> findRecipientListById(Long id) {
        return recipientListRepository.findById(id);
    }
    
    // Recipient Management
    public Recipient addRecipient(RecipientDto dto, User user) {
        RecipientList recipientList = recipientListRepository.findById(dto.getRecipientListId())
                .orElseThrow(() -> new RuntimeException("Recipient list not found"));
        
        if (!recipientList.getUser().getId().equals(user.getId()) && user.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("Access denied");
        }
        
        if (recipientRepository.existsByEmailAndRecipientList(dto.getEmail(), recipientList)) {
            throw new RuntimeException("Email already exists in this list");
        }
        
        Recipient recipient = new Recipient();
        recipient.setEmail(dto.getEmail());
        recipient.setFirstName(dto.getFirstName());
        recipient.setLastName(dto.getLastName());
        recipient.setStatus(Recipient.RecipientStatus.ACTIVE);
        recipient.setRecipientList(recipientList);
        
        Recipient savedRecipient = recipientRepository.save(recipient);
        
        systemLogService.logUserAction(user, "RECIPIENT_ADDED", 
                "Recipient added: " + savedRecipient.getEmail() + " to list: " + recipientList.getName(), 
                null, null);
        
        return savedRecipient;
    }
    
    public Recipient updateRecipient(Long id, RecipientDto dto, User user) {
        Recipient recipient = recipientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recipient not found"));
        
        if (!recipient.getRecipientList().getUser().getId().equals(user.getId()) && 
            user.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("Access denied");
        }
        
        recipient.setEmail(dto.getEmail());
        recipient.setFirstName(dto.getFirstName());
        recipient.setLastName(dto.getLastName());
        recipient.setStatus(dto.getStatus());
        
        return recipientRepository.save(recipient);
    }
    
    public void deleteRecipient(Long id, User user) {
        Recipient recipient = recipientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recipient not found"));
        
        if (!recipient.getRecipientList().getUser().getId().equals(user.getId()) && 
            user.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("Access denied");
        }
        
        recipientRepository.delete(recipient);
        
        systemLogService.logUserAction(user, "RECIPIENT_DELETED", 
                "Recipient deleted: " + recipient.getEmail(), null, null);
    }
    
    public Page<Recipient> getRecipientsByList(Long listId, Pageable pageable) {
        RecipientList recipientList = recipientListRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("Recipient list not found"));
        return recipientRepository.findByRecipientList(recipientList, pageable);
    }
    
    public void unsubscribeRecipient(String email, Long listId) {
        RecipientList recipientList = recipientListRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("Recipient list not found"));
        
        recipientRepository.findByEmailAndRecipientList(email, recipientList)
                .ifPresent(recipient -> {
                    recipient.setStatus(Recipient.RecipientStatus.UNSUBSCRIBED);
                    recipient.setUnsubscribedAt(java.time.LocalDateTime.now());
                    recipientRepository.save(recipient);
                });
    }
    
    // CSV Import/Export
    public List<Recipient> importRecipientsFromCsv(Long listId, MultipartFile file, User user) throws IOException {
        RecipientList recipientList = recipientListRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("Recipient list not found"));
        
        if (!recipientList.getUser().getId().equals(user.getId()) && user.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("Access denied");
        }
        
        List<Recipient> recipients = new ArrayList<>();
        
        try (CSVParser parser = CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .parse(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            
            for (CSVRecord record : parser) {
                String email = record.get("email");
                String firstName = record.get("firstName");
                String lastName = record.get("lastName");
                
                if (!recipientRepository.existsByEmailAndRecipientList(email, recipientList)) {
                    Recipient recipient = new Recipient();
                    recipient.setEmail(email);
                    recipient.setFirstName(firstName);
                    recipient.setLastName(lastName);
                    recipient.setStatus(Recipient.RecipientStatus.ACTIVE);
                    recipient.setRecipientList(recipientList);
                    
                    recipients.add(recipientRepository.save(recipient));
                }
            }
        }
        
        systemLogService.logUserAction(user, "RECIPIENTS_IMPORTED", 
                "Imported " + recipients.size() + " recipients to list: " + recipientList.getName(), 
                null, null);
        
        log.info("Imported {} recipients to list: {} by user: {}", 
                recipients.size(), recipientList.getName(), user.getUsername());
        
        return recipients;
    }
    
    public String exportRecipientsToCsv(Long listId, User user) throws IOException {
        RecipientList recipientList = recipientListRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("Recipient list not found"));
        
        if (!recipientList.getUser().getId().equals(user.getId()) && user.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("Access denied");
        }
        
        StringWriter writer = new StringWriter();
        try (CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT
                .withHeader("email", "firstName", "lastName", "status", "subscribedAt"))) {
            
            List<Recipient> recipients = recipientRepository.findByRecipientList(recipientList, Pageable.unpaged()).getContent();
            
            for (Recipient recipient : recipients) {
                printer.printRecord(
                        recipient.getEmail(),
                        recipient.getFirstName(),
                        recipient.getLastName(),
                        recipient.getStatus(),
                        recipient.getSubscribedAt()
                );
            }
        }
        
        systemLogService.logUserAction(user, "RECIPIENTS_EXPORTED", 
                "Exported recipients from list: " + recipientList.getName(), null, null);
        
        return writer.toString();
    }
    
    public long getActiveRecipientCount(Long listId) {
        return recipientListRepository.countActiveRecipientsByListId(listId);
    }
}
