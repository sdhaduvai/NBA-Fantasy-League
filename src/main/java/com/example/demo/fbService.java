package com.example.demo;

import java.util.ArrayList;
import java.util.List;

import javax.transaction.Transactional;
import org.springframework.stereotype.Service;
import com.example.demo.FacebookDB;

@Service
@Transactional
public class fbService {

    private final FacebookDBRepository fbrepository;

    public fbService(FacebookDBRepository fbrepository) {
        this.fbrepository=fbrepository;
    }

    public List<FacebookDB> findAllUsers(){
        List<FacebookDB> allFBUsers = new ArrayList<FacebookDB>();
        for(FacebookDB x : fbrepository.findAll()) {
            System.out.println(x.name);
            allFBUsers.add(x);
        }
        return allFBUsers;
    }
}