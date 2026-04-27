# Flight Plan DSL Specification (Informal)

## Purpose
This document describes the informal lexical and syntactic specification for the Flight Plan DSL.

## Lexical Elements
- **Keywords**: `FLIGHT`, `TYPE`, `REGULAR`, `CHARTER`, `LEG`, `DEPARTURE`, `ARRIVAL`, `ROUTE`, `SEGMENT`, `FUEL`, `ALTITUDE`, `WIDTH`, `WIND`.
- **Units**: `KG`, `L`, `M`, `M/S`.
- **Airport codes**:
  - `IATA_CODE`: exactly 3 uppercase letters (example: `OPO`).
  - `ICAO_CODE`: exactly 4 uppercase letters (example: `LPPR`).
- **Date**: `YYYY-MM-DD`.
- **Time**: `HH:MM`.
- **Number**: integer or decimal (`42`, `12.5`).
- **Identifier**: starts with a letter, then letters/digits/`_`/`-`.
- **Coordinates**: `(<signedNumber>,<signedNumber>)`.

## Syntactic Structure
At a high level, a valid DSL file contains:
1. One `FLIGHT` declaration with identifier and flight type.
2. One or more `LEG` blocks.
3. Each `LEG` includes, in this order:
   - `DEPARTURE ...;`
   - `ARRIVAL ...;`
   - `ROUTE ... -> ...;`
   - one or more `SEGMENT ...;`
   - `FUEL ...;`

## Valid Example
```text
FLIGHT TP123 TYPE REGULAR {
  LEG {
    DEPARTURE OPO 2026-06-01 10:00;
    ARRIVAL LIS 2026-06-01 10:45;
    ROUTE OPO -> LIS;
    SEGMENT (+41.15,-8.61) -> (+38.72,-9.14) ALTITUDE 10000 M WIDTH 2000 M WIND 12 180 M/S;
    FUEL 5300 KG;
  }
}
```

## Invalid Example
Missing semicolon after `DEPARTURE`:
```text
DEPARTURE OPO 2026-06-01 10:00
ARRIVAL LIS 2026-06-01 10:45;
```
The parser reports a syntax error with line/column details.
