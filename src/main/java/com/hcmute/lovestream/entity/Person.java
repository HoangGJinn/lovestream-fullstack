package com.hcmute.lovestream.entity;

import com.hcmute.lovestream.entity.enums.CreditType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Person {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String fullName;

    @Enumerated(EnumType.STRING)
    private CreditType creditType;
}
