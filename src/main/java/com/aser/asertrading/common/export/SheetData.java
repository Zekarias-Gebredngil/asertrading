package com.aser.asertrading.common.export;

import java.util.List;

public record SheetData(
        String sheetName,
        List<String> headers,
        List<List<Object>> rows
) {}

