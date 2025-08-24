package com.github.arburk.stockalert.application.domain;

import java.util.ArrayList;

public record StockAlertDb(
    ArrayList<Security> securities,
    MetaInfo metaInfo
) {

}
