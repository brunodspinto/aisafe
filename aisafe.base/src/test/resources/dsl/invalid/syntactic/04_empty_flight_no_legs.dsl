// Invalid (SYNTACTIC): flight block has no legs
// Grammar rule: flight = FLIGHT id TYPE type { leg+ }
// 'leg+' requires at least one leg; empty braces are not allowed
// Expected: REJECTED - syntax error at '}', parser expected LEG
FLIGHT TP123 TYPE REGULAR {
}
