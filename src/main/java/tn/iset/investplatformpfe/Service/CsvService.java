package tn.iset.investplatformpfe.Service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tn.iset.investplatformpfe.Entity.Prospect;
import tn.iset.investplatformpfe.Repository.ProspectRepository;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class CsvService {

    private final ProspectRepository repo;  // ← injecter le repo

    public CsvService(ProspectRepository repo) {
        this.repo = repo;
    }

    public List<Prospect> parse(MultipartFile file) throws Exception {
        List<Prospect> list = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
        String line;
        reader.readLine(); // skip header

        while ((line = reader.readLine()) != null) {
            String[] data = line.split(";");
            if (data.length < 6) continue;
            if (data[1] == null || !data[1].contains("@")) continue;

            String email = data[1].trim();

            // ← NE PAS réimporter si l'email existe déjà en base
            if (repo.existsByEmail(email)) continue;

            Prospect p = new Prospect();
            p.setName(data[0].trim());
            p.setEmail(email);
            p.setCategory(data[2].trim());
            p.setCompany(data[3].trim());
            p.setCity(data[4].trim());
            p.setInterestLevel(data[5].trim());
            p.setStatus("PENDING");
            list.add(p);
        }
        return list;
    }
}