// DrugWarningsSection.jsx
// Drop into pages/patient/PrescriptionDetail.jsx (or render as a sub-component)
// Props: { drugInteractions, drugDiseaseWarnings, highSeverityCount, moderateSeverityCount }
//
// Key fix: all item property access uses camelCase (drug1Name, drug2Name, drugName,
// indicationText, meshTerms) to match Jackson's default serialization of PrescriptionResponse.

import React, { useState } from "react";

// ── Severity badge ────────────────────────────────────────────────────────────
const SeverityBadge = ({ severity }) => {
  const styles = {
    HIGH:     { background: "#fef2f2", color: "#991b1b", border: "1px solid #fca5a5" },
    MODERATE: { background: "#fffbeb", color: "#92400e", border: "1px solid #fcd34d" },
    LOW:      { background: "#f0fdf4", color: "#166534", border: "1px solid #86efac" },
    UNKNOWN:  { background: "#f8fafc", color: "#475569", border: "1px solid #cbd5e1" },
  };
  const s = styles[severity] || styles.UNKNOWN;
  return (
    <span style={{
      ...s,
      padding: "2px 8px",
      borderRadius: "9999px",
      fontSize: "11px",
      fontWeight: 600,
      textTransform: "uppercase",
      letterSpacing: "0.04em",
      whiteSpace: "nowrap",
    }}>
      {severity}
    </span>
  );
};

// ── Collapsible section wrapper ───────────────────────────────────────────────
const Section = ({ title, count, countColor, children, defaultOpen = false }) => {
  const [open, setOpen] = useState(defaultOpen);
  return (
    <div style={{ marginBottom: "16px", border: "1px solid #e2e8f0", borderRadius: "10px", overflow: "hidden" }}>
      <button
        onClick={() => setOpen(o => !o)}
        style={{
          width: "100%", display: "flex", alignItems: "center", justifyContent: "space-between",
          padding: "12px 16px", background: "#f8fafc", border: "none", cursor: "pointer",
          textAlign: "left",
        }}
      >
        <span style={{ fontWeight: 600, fontSize: "14px", color: "#1e293b" }}>{title}</span>
        <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
          {count > 0 && (
            <span style={{
              background: countColor || "#3b82f6",
              color: "#fff", borderRadius: "9999px",
              padding: "1px 8px", fontSize: "12px", fontWeight: 600,
            }}>{count}</span>
          )}
          <span style={{ color: "#64748b", fontSize: "12px" }}>{open ? "▲" : "▼"}</span>
        </div>
      </button>
      {open && <div style={{ padding: "12px 16px" }}>{children}</div>}
    </div>
  );
};

// ── Main component ────────────────────────────────────────────────────────────
const DrugWarningsSection = ({
  drugInteractions = [],
  drugDiseaseWarnings = [],
  highSeverityCount = 0,
  moderateSeverityCount = 0,
}) => {

  if (drugInteractions.length === 0 && drugDiseaseWarnings.length === 0) {
    return (
      <div style={{
        padding: "14px 16px", borderRadius: "10px",
        background: "#f0fdf4", border: "1px solid #86efac",
        color: "#166534", fontSize: "14px",
      }}>
        ✓ No drug interactions or disease warnings found.
      </div>
    );
  }

  return (
    <div>
      {/* ── Summary bar ─────────────────────────────────────────────────── */}
      {(highSeverityCount > 0 || moderateSeverityCount > 0) && (
        <div style={{
          display: "flex", gap: "10px", marginBottom: "14px",
          padding: "10px 14px", borderRadius: "8px",
          background: highSeverityCount > 0 ? "#fef2f2" : "#fffbeb",
          border: `1px solid ${highSeverityCount > 0 ? "#fca5a5" : "#fcd34d"}`,
        }}>
          <span style={{ fontSize: "13px", color: "#374151" }}>
            <strong>Interaction summary: </strong>
            {highSeverityCount > 0 && (
              <span style={{ color: "#991b1b", marginRight: "10px" }}>
                {highSeverityCount} HIGH
              </span>
            )}
            {moderateSeverityCount > 0 && (
              <span style={{ color: "#92400e" }}>
                {moderateSeverityCount} MODERATE
              </span>
            )}
          </span>
        </div>
      )}

      {/* ── Drug-Drug Interactions ────────────────────────────────────── */}
      {drugInteractions.length > 0 && (
        
        <Section
            title="Drug-Drug Interactions"
            count={drugInteractions.length}
            countColor={
            highSeverityCount > 0 ? "#dc2626" :
            moderateSeverityCount > 0 ? "#d97706" : "#64748b"
            }
            defaultOpen={highSeverityCount > 0 || moderateSeverityCount > 0}
        >
          <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
            {drugInteractions.map((item, idx) => (
              <div key={idx} style={{
                padding: "10px 12px", borderRadius: "8px",
                background: "#fff", border: "1px solid #e2e8f0",
              }}>
                <div style={{ display: "flex", alignItems: "center", gap: "8px", marginBottom: "6px" }}>
                  {/* FIX: camelCase — matches Jackson serialization of DrugInteractionResult */}
                  <span style={{ fontWeight: 600, fontSize: "13px", color: "#1e293b" }}>
                    {item.drug1Name}
                  </span>
                  <span style={{ color: "#94a3b8", fontSize: "12px" }}>+</span>
                  <span style={{ fontWeight: 600, fontSize: "13px", color: "#1e293b" }}>
                    {item.drug2Name}
                  </span>
                  <div style={{ marginLeft: "auto" }}>
                    <SeverityBadge severity={item.severity} />
                  </div>
                </div>
                {item.description && (
                  <p style={{ margin: 0, fontSize: "12px", color: "#475569", lineHeight: "1.5" }}>
                    {item.description}
                  </p>
                )}
                <div style={{ marginTop: "6px", fontSize: "11px", color: "#94a3b8" }}>
                  Source: {item.source || "DrugBank"}
                </div>
              </div>
            ))}
          </div>
        </Section>
      )}

      {/* ── Drug → Disease Indications ───────────────────────────────── */}
      {drugDiseaseWarnings.length > 0 && (
        <Section
          title="Drug Indications & Disease Categories"
          count={drugDiseaseWarnings.length}
          countColor="#6366f1"
          defaultOpen={false}
        >
          <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
            {drugDiseaseWarnings.map((item, idx) => (
              <div key={idx} style={{
                padding: "10px 12px", borderRadius: "8px",
                background: "#fff", border: "1px solid #e2e8f0",
              }}>
                <div style={{ fontWeight: 600, fontSize: "13px", color: "#1e293b", marginBottom: "6px" }}>
                  {/* FIX: camelCase — matches Jackson serialization of DrugDiseaseWarning */}
                  {item.drugName}
                  {item.rxcui && (
                    <span style={{ fontWeight: 400, color: "#94a3b8", fontSize: "11px", marginLeft: "6px" }}>
                      RxCUI: {item.rxcui}
                    </span>
                  )}
                </div>

                {/* FIX: camelCase indicationText */}
                {item.indicationText && (
                  <p style={{ margin: "0 0 8px 0", fontSize: "12px", color: "#475569", lineHeight: "1.5" }}>
                    {item.indicationText.length > 300
                      ? item.indicationText.slice(0, 300) + "…"
                      : item.indicationText}
                  </p>
                )}

                {/* FIX: camelCase meshTerms */}
                {item.meshTerms?.length > 0 && (
                  <div style={{ display: "flex", flexWrap: "wrap", gap: "4px" }}>
                    {item.meshTerms.slice(0, 8).map((term, i) => (
                      <span key={i} style={{
                        background: "#ede9fe", color: "#5b21b6",
                        border: "1px solid #c4b5fd",
                        borderRadius: "9999px", padding: "2px 8px",
                        fontSize: "11px",
                      }}>
                        {term}
                      </span>
                    ))}
                    {item.meshTerms.length > 8 && (
                      <span style={{ fontSize: "11px", color: "#94a3b8", alignSelf: "center" }}>
                        +{item.meshTerms.length - 8} more
                      </span>
                    )}
                  </div>
                )}
              </div>
            ))}
          </div>
        </Section>
      )}
    </div>
  );
};

export default DrugWarningsSection;