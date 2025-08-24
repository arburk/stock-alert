package com.github.arburk.stockalert.application.domain;

import java.time.LocalDateTime;

public record MetaInfo (
    LocalDateTime lastErrorAlert
){
}
