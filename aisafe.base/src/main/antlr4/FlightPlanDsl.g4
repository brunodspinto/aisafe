grammar FlightPlanDsl;

flightPlan
    : flight+ EOF
    ;

// ── Flight ──────────────────────────────────────────────────────────────────
// A flight has a unique identifier, a type, and one or more legs.
// Format of identifier: 2-letter airline code + 1-4 digits + optional suffix
// e.g. AA123, TP1234A

flight
    : FLIGHT IDENTIFIER TYPE flightType LBRACE leg+ RBRACE
    ;

flightType
    : REGULAR
    | CHARTER
    ;

// ── Leg ─────────────────────────────────────────────────────────────────────
// Each leg is a non-stop journey between two airports.
// Must have: departure, arrival, route, fuel, and at least one segment.

leg
    : LEG LBRACE
        departure
        arrival
        route
        segment+
        fuel
      RBRACE
    ;

departure
    : DEPARTURE COLON airportCode dateTime SEMI
    ;

arrival
    : ARRIVAL COLON airportCode dateTime SEMI
    ;

// ── Route ────────────────────────────────────────────────────────────────────
// Defines origin -> destination of the leg

route
    : ROUTE COLON airportCode ARROW airportCode SEMI
    ;


// ── Segment ──────────────────────────────────────────────────────────────────
// A linear path connecting two coordinates.
// Fields from statement section 3.2:
//   - start/end coordinates
//   - allowed altitude slots
//   - width per altitude slot
//   - wind direction (angle relative to North) and speed

segment
    : SEGMENT LBRACE
        START COLON coordinate SEMI
        END   COLON coordinate SEMI
        altitudeSlot+
        windDecl
      RBRACE
    ;


altitudeSlot
    : ALTITUDE COLON altitude WIDTH COLON distance SEMI
    ;

windDecl
    : WIND COLON LPAREN windDirection COMMA windSpeed RPAREN SEMI
    ;

windDirection
    : signedNumber
    ;

windSpeed
    : signedNumber unit
    ;


fuel
    : FUEL COLON signedNumber fuelUnit SEMI
    ;

fuelUnit
    : KG
    | L
    ;

coordinate
    : LPAREN signedNumber COMMA signedNumber RPAREN
    ;

altitude
    : signedNumber unit
    ;


distance
    : signedNumber unit
    ;

airportCode
    : IATA_CODE
    | ICAO_CODE
    ;

signedNumber
    : (PLUS | MINUS)? NUMBER
    ;

dateTime
    : DATE TIME
    ;

unit
    : M
    | KM
    | FT
    | KNOT
    | MPS
    ;
// ═══════════════════════════════════════════════════════════════════════════
// LEXER RULES
// ═══════════════════════════════════════════════════════════════════════════

// ── Keywords (case-insensitive via fragments) ────────────────────────────────

FLIGHT      : F L I G H T ;
TYPE        : T Y P E ;
REGULAR     : R E G U L A R ;
CHARTER     : C H A R T E R ;
LEG         : L E G ;
DEPARTURE   : D E P A R T U R E ;
ARRIVAL     : A R R I V A L ;
ROUTE       : R O U T E ;
SEGMENT     : S E G M E N T ;
START       : S T A R T ;
END         : E N D ;
ALTITUDE    : A L T I T U D E ;
WIDTH       : W I D T H ;
WIND        : W I N D ;
FUEL        : F U E L ;

// ── Units ────────────────────────────────────────────────────────────────────

KG          : K G ;
L           : L_CHAR ;
M           : M_CHAR ;
KM          : K M_CHAR ;
FT          : F T ;
KNOT        : K N O T ;
MPS         : M_CHAR '/' S ;

// ── Airport codes ─────────────────────────────────────────────────────────────
// ICAO must come before IATA (longer match wins)

ICAO_CODE   : UPPER UPPER UPPER UPPER ;
IATA_CODE   : UPPER UPPER UPPER ;

// ── Flight identifier ─────────────────────────────────────────────────────────
// 2 uppercase letters + 1-4 digits + optional 1-letter suffix
// e.g. AA123, TP1234, TP1234A

IDENTIFIER  : UPPER UPPER DIGIT DIGIT? DIGIT? DIGIT? LETTER? ;


DATE        : DIGIT DIGIT DIGIT DIGIT '-' DIGIT DIGIT '-' DIGIT DIGIT ;
TIME        : DIGIT DIGIT ':' DIGIT DIGIT ;


NUMBER      : DIGIT+ ('.' DIGIT+)? ;


ARROW       : '->' ;
LBRACE      : '{' ;
RBRACE      : '}' ;
LPAREN      : '(' ;
RPAREN      : ')' ;
SEMI        : ';' ;
COLON       : ':' ;
COMMA       : ',' ;
PLUS        : '+' ;
MINUS       : '-' ;


WS              : [ \t\r\n]+    -> skip ;
LINE_COMMENT    : '//' ~[\r\n]* -> skip ;
BLOCK_COMMENT   : '/*' .*? '*/' -> skip ;


fragment DIGIT  : [0-9] ;
fragment UPPER  : [A-Z] ;
fragment LETTER : [a-zA-Z] ;

fragment A : [aA] ;
fragment B : [bB] ;
fragment C : [cC] ;
fragment D : [dD] ;
fragment E : [eE] ;
fragment F : [fF] ;
fragment G : [gG] ;
fragment H : [hH] ;
fragment I : [iI] ;
fragment J : [jJ] ;
fragment K : [kK] ;
fragment L_CHAR : [lL] ;
fragment M_CHAR : [mM] ;
fragment N : [nN] ;
fragment O : [oO] ;
fragment P : [pP] ;
fragment Q : [qQ] ;
fragment R : [rR] ;
fragment S : [sS] ;
fragment T : [tT] ;
fragment U : [uU] ;
fragment V : [vV] ;
fragment W : [wW] ;
fragment X : [xX] ;
fragment Y : [yY] ;
fragment Z : [zZ] ;
