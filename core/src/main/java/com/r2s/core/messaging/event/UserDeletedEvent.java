package com.r2s.core.messaging.event;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDeletedEvent {
    private String username;
}
