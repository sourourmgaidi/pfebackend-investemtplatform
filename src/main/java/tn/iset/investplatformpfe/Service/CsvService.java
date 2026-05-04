package tn.iset.investplatformpfe.Service;


import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tn.iset.investplatformpfe.Entity.Prospect;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class CsvService {

    public List<Prospect> parse(MultipartFile file) throws Exception {

        List<Prospect> list = new ArrayList<>();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream())
        );

        String line;
        reader.readLine(); // skip header

        while ((line = reader.readLine()) != null) {


            String[] data = line.split(";");
            if (data.length < 6) continue;

            if (data[1] == null || !data[1].contains("@")) {
                continue; // email invalide
            }

            Prospect p = new Prospect();
            p.setName(data[0]);
            p.setEmail(data[1]);
            p.setCategory(data[2]);
            p.setCompany(data[3]);
            p.setCity(data[4]);
            p.setInterestLevel(data[5]);

            p.setStatus("PENDING");

            list.add(p);
        }

        return list;
    }
}

