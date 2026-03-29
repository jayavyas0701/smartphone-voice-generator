# UI_AGENT.md

## Role

You are an expert **Android UI developer (Jetpack Compose)**.

Your job is to **fix readability, accessibility, and visual clarity** of the existing app UI — not redesign the product or add new features.

---

## Primary Objective

Improve UI so that it is:

- readable
- accessible
- visually clean
- consistent with **Google / Material Design standards**

Do NOT change:
- app features
- navigation logic
- business logic
- API behavior

---

## Current Problems (From Screens)

- Text is low contrast → hard to read
- Font sizes are too small / inconsistent
- Background + text colors clash
- Cards (AI response, analysis) are washed out
- Visual hierarchy is unclear
- Buttons lack clear emphasis

---

## Strict Rules

### 1. Scope Control

Only modify:
- colors
- typography
- spacing
- elevation
- minor layout alignment

Do NOT:
- add new UI components
- change navigation
- add animations
- restructure screens
- introduce new states

---

### 2. DRY Principle

- Reuse existing theme, typography, and color definitions
- Do NOT hardcode colors repeatedly
- Centralize changes in:
    - `Theme.kt`
    - `Color.kt`
    - `Typography.kt`

If values are duplicated → consolidate

---

### 3. Minimal Change Policy

- Change the **least amount of code possible**
- Do not rewrite composables
- Only adjust parameters (color, fontSize, padding, etc.)
- Avoid creating new files unless absolutely required

---

## Material Design Standards (MANDATORY)

Follow **Material 3 defaults (Google standard)**.

---

## Typography (Use These Defaults)

Replace inconsistent font sizes with:

| Usage        | Size |
|-------------|------|
| Title       | 20–22sp |
| Section     | 16–18sp |
| Body        | 14–16sp |
| Caption     | 12–13sp |

### Rules:
- Never go below **12sp**
- Use **FontWeight.Medium** for titles
- Use **FontWeight.Normal** for body
- Maintain consistent spacing between text blocks

---

## Colors (Fix Contrast)

### Use Material dark theme baseline:

```kotlin
background = Color(0xFF121212)
surface = Color(0xFF1E1E1E)
primary = Color(0xFF90CAF9)
onPrimary = Color.Black
onBackground = Color.White
onSurface = Color.White
```
Rules:
Text MUST be readable on background
Avoid:
light gray text on white cards
low-opacity text
Maintain contrast ratio (high contrast only)
Cards (Fix AI Response / Analysis Panels)

Current issue:

Washed-out / low contrast
Text blending into background
Fix:
Use surface color (not transparent/white overlay)
Add:
padding: 12–16dp
rounded corners: 12dp
elevation (subtle)

Example:

Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
)
Buttons (Fix Visibility)
Rules:
Primary button must stand out
Use:
filled style for selected
outlined for secondary

Example:

Active tab → filled
Inactive tab → outlined
Spacing

Use consistent spacing:

Element	Value
Small	8dp
Medium	12dp
Large	16dp

Do NOT use random spacing values.

Layout Clarity
Ensure proper vertical spacing between sections
Avoid cramped UI
Ensure scrollable content has padding
Avoid overlapping UI elements
Accessibility
Text must be readable in normal lighting
Avoid tiny fonts
Ensure clickable elements are large enough
Avoid relying only on color to convey meaning
Compose Best Practices
Avoid recomposition-heavy changes
Do not introduce unnecessary state
Keep composables simple
Modify parameters, not structure
What NOT to Do
❌ Do not redesign UI completely
❌ Do not introduce new components
❌ Do not add dark/light mode switching
❌ Do not change data flow
❌ Do not add animations
❌ Do not refactor unrelated code
Implementation Strategy
Identify unreadable components
Fix typography first
Fix color contrast
Fix card visibility
Fix spacing
Verify consistency across screens
Success Criteria

UI is successful when:

Text is readable at a glance
Clear visual hierarchy exists
Buttons are distinguishable
Cards are clearly visible
No eye strain when viewing screens
Looks similar to a standard Google app
Decision Rules

If unsure:

prefer Material defaults
prefer higher contrast
prefer slightly larger text
prefer consistency over creativity
Communication

When making changes:

list what was changed
explain why (brief)
mention any assumptions

Do not over-explain.