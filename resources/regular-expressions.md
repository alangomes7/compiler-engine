# Documentation – New Regular Expressions (ER)

This document explains the new regular expressions used for numbers, strings, identifiers, and booleans.

---

# 1. Numbers

## Regular Expression

```text
[ - ]? [ [ 0-9 ]+ [ . [ 0-9 ]* ]? | [ . [ 0-9 ]+ ] ] [ [ e / E ] [ - ]? [ 0-9 ]+ ]?
```

## Description

A number may contain:

* An optional negative sign (`-`)
* An integer or decimal part
* An optional scientific notation exponent using `e` or `E`

## Accepted Formats

### Integer Numbers

```text
0
15
999
```

### Decimal Numbers

```text
10.5
3.
0.25
.75
```

### Negative Numbers

```text
-1
-3.14
-.5
```

### Scientific Notation

```text
1e10
2E5
-3.14e8
0.5E-3
.75e2
```

## Notes

* The minus sign is optional.
* A decimal number may omit digits before the decimal point:

  * Example: `.5`
* A decimal number may omit digits after the decimal point if there are digits before it:

  * Example: `10.`
* The exponent part is optional.
* The exponent may also contain an optional minus sign.

---

# 2. Strings

## Regular Expression

```text
" [ [ ^ " \ ] | [ \ [ " / \ / n / r / t ] ] ]* "
```

## Description

A string:

* Starts with a double quote (`"`)
* Ends with a double quote (`"`)
* May contain normal characters
* May contain supported escape sequences

## Allowed Escape Sequences

| Escape | Meaning         |
| ------ | --------------- |
| `\"`   | Double quote    |
| `\\`   | Backslash       |
| `\/`   | Slash           |
| `\n`   | New line        |
| `\r`   | Carriage return |
| `\t`   | Tab             |

## Valid Examples

```text
"hello"
"Hello World"
"line\nnext"
"tab\tspace"
"quote: \"test\""
"path\\folder"
```

## Invalid Examples

```text
"unfinished
"invalid \x escape"
"text " inside"
```

## Notes

* Double quotes inside the string must be escaped.
* Backslashes must also be escaped.
* Only the listed escape sequences are valid.

---

# 3. Identifiers (`<id>`)

## Regular Expression

```text
[ a-zA-Z!$%&*\/:<=>?^_~ ] [ a-zA-Z0-9!$%&*\/:<=>?^_~+\-.@ ]*
```

## Description

An identifier:

* Must begin with a letter or one of the allowed special characters
* May continue with letters, digits, or additional allowed special characters

## Allowed First Characters

```text
a-z
A-Z
! $ % & * / : < = > ? ^ _ ~
```

## Allowed Remaining Characters

```text
a-z
A-Z
0-9
! $ % & * / : < = > ? ^ _ ~ + - . @
```

## Valid Examples

```text
name
value1
_result
x-y
user.name
calc@home
!temp
sum+value
```

## Invalid Examples

```text
1value
@test
-value
.space
```

## Notes

* Identifiers cannot start with digits.
* Characters such as `+`, `-`, `.`, and `@` are allowed only after the first character.

---

# 4. Booleans (`<bool>`)

## Regular Expression

```text
# [ t / T / f / F ]
```

## Description

A boolean value:

* Starts with `#`
* Is followed by one of:

  * `t`
  * `T`
  * `f`
  * `F`

## Valid Examples

```text
#t
#T
#f
#F
```

## Meaning

| Value | Meaning |
| ----- | ------- |
| `#t`  | true    |
| `#T`  | true    |
| `#f`  | false   |
| `#F`  | false   |

---

# Summary Table

| Category    | Example Values                |
| ----------- | ----------------------------- |
| Numbers     | `10`, `-3.5`, `.8`, `1e5`     |
| Strings     | `"hello"`, `"line\nnext"`     |
| Identifiers | `name`, `_value`, `user.name` |
| Booleans    | `#t`, `#F`                    |
