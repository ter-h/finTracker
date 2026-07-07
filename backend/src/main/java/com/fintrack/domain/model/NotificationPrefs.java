package com.fintrack.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "notification_prefs")
@Getter
@Setter
@NoArgsConstructor
public class NotificationPrefs {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "budget_alert_80")
    private boolean budgetAlert80  = true;
    @Column(name = "budget_alert_100")
    private boolean budgetAlert100 = true;
    private boolean weeklySummary  = false;
    private String  alertEmail;   // if null, use the user's main email
}