import React from "react";

interface BadgeProps {
  children: React.ReactNode;
  variant?: "reviewed" | "needs-review" | "info" | "neutral" | "accent";
  size?: "sm" | "md";
  className?: string;
  dotColor?: string;
}

export function Badge({
  children,
  variant = "neutral",
  size = "md",
  className = "",
  dotColor,
}: BadgeProps) {
  const baseStyle: React.CSSProperties = {
    display: "inline-flex",
    alignItems: "center",
    gap: "5px",
    borderRadius: "9999px",
    fontFamily: "var(--font-mono)",
    fontWeight: 600,
    textTransform: "uppercase",
    letterSpacing: "0.03em",
    whiteSpace: "nowrap",
    padding: size === "sm" ? "2px 7px" : "3px 10px",
    fontSize: size === "sm" ? "0.62rem" : "0.7rem",
    lineHeight: 1.3,
  };

  const variantStyles: Record<string, React.CSSProperties> = {
    reviewed: {
      background: "var(--accent-emerald-subtle)",
      color: "var(--accent-emerald)",
      border: "1px solid var(--accent-emerald)",
    },
    "needs-review": {
      background: "var(--accent-amber-subtle)",
      color: "var(--accent-amber)",
      border: "1px solid var(--accent-amber)",
    },
    info: {
      background: "var(--accent-cyan-subtle)",
      color: "var(--accent-cyan)",
      border: "1px solid var(--accent-cyan)",
    },
    neutral: {
      background: "var(--bg-surface-elevated)",
      color: "var(--text-secondary)",
      border: "1px solid var(--border-color)",
    },
    accent: {
      background: "var(--accent-cyan-subtle)",
      color: "var(--accent-cyan)",
      border: "1px solid var(--accent-cyan)",
    },
  };

  return (
    <span
      className={`core-badge ${className}`}
      style={{ ...baseStyle, ...variantStyles[variant] }}
    >
      {dotColor && (
        <span
          style={{
            width: "6px",
            height: "6px",
            borderRadius: "50%",
            backgroundColor: dotColor,
            flexShrink: 0,
          }}
        />
      )}
      {children}
    </span>
  );
}
