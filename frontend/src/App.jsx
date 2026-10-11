import { useEffect, useState } from "react";

import "./App.css";

const API = "http://localhost:8080/api";

const ACCOUNT_KEY = "ironcoreAccount";

async function api(path, options = {}) {

  const response = await fetch(`${API}${path}`, {

    ...options,

    headers: {

      "Content-Type": "application/json",

      ...(options.headers || {}),

    },

  });

  const text = await response.text();

  let data = null;

  try {

    data = text ? JSON.parse(text) : null;

  } catch {

    data = text;

  }

  if (!response.ok) {

    throw new Error(

      data?.message || data?.error || data || `Request failed: ${response.status}`

    );

  }

  return data;

}

const styles = {

  page: {

    minHeight: "100vh",

    background: "#f5f7f6",

    color: "#26382b",

    padding: "24px",

    fontFamily: "Arial, sans-serif",

  },

  container: { maxWidth: "1400px", margin: "0 auto" },

  panel: {

    background: "#fff",

    border: "1px solid #e0e7e1",

    borderRadius: "10px",

    padding: "24px",

    marginBottom: "20px",

    boxShadow: "0 2px 8px rgba(0,0,0,0.04)",

  },

  input: {

    width: "100%",

    boxSizing: "border-box",

    padding: "10px",

    borderRadius: "6px",

    border: "1px solid #cbd5cc",

    background: "#fff",

    color: "#26382b",

    margin: "4px 0",

  },

  button: {

    padding: "10px 15px",

    borderRadius: "6px",

    border: "none",

    background: "#23834a",

    color: "#fff",

    fontWeight: "bold",

    cursor: "pointer",

    margin: "3px",

  },

  secondary: {

    padding: "9px 13px",

    borderRadius: "6px",

    border: "1px solid #cbd5cc",

    background: "#fff",

    color: "#26382b",

    cursor: "pointer",

    margin: "3px",

  },

  muted: { color: "#69766b" },

  tableWrap: { width: "100%", overflowX: "auto" },

  table: {

    width: "100%",

    minWidth: "1050px",

    borderCollapse: "collapse",

  },

  cell: {

    padding: "12px 10px",

    borderBottom: "1px solid #e5eae6",

    textAlign: "left",

    verticalAlign: "middle",

  },

};

function readAccount() {

  try {

    return JSON.parse(localStorage.getItem(ACCOUNT_KEY) || "null");

  } catch {

    return null;

  }

}

function Field({ label, ...props }) {

  return (

    <label style={{ display: "block", marginBottom: "12px" }}>

      {label}

      <input style={styles.input} {...props} />

    </label>

  );

}

export default function App() {

  const [account, setAccount] = useState(readAccount);

  const [mode, setMode] = useState("login");

  const [form, setForm] = useState({ fullName: "", email: "", password: "" });

  const [members, setMembers] = useState([]);

  const [payments, setPayments] = useState([]);

  const [userPayments, setUserPayments] = useState([]);

  const [plans, setPlans] = useState([]);

  const [memberships, setMemberships] = useState([]);

  const [memberRecord, setMemberRecord] = useState(null);

  const [paymentEdits, setPaymentEdits] = useState({});

  const [paymentEditMode, setPaymentEditMode] = useState({});

  const [newPayments, setNewPayments] = useState({});

  const [savingPayment, setSavingPayment] = useState({});

  const [memberEdits, setMemberEdits] = useState({});

  const [savingMember, setSavingMember] = useState({});

  const [profile, setProfile] = useState({ fullName: "", age: "", phone: "" });

  const [profileEditing, setProfileEditing] = useState(true);

  const [loading, setLoading] = useState(false);

  const [planSubmitting, setPlanSubmitting] = useState(false);

  const [message, setMessage] = useState("");

  const [error, setError] = useState("");

  const isAdmin = account?.role?.toUpperCase() === "ADMIN";

  function notifyError(err) {

    setError(err?.message || "Something went wrong.");

    setMessage("");

  }

  function lockKey(user) {

    return `ironcoreProfileLocked:${user?.id ?? user?.email ?? "user"}`;

  }

  async function ensureMember(user) {

    const all = await api("/members");

    const existing = all.find(

      (m) => m.email?.toLowerCase() === user.email?.toLowerCase()

    );

    if (existing) return existing;

    return api("/members", {

      method: "POST",

      body: JSON.stringify({

        fullName: user.fullName || "Gym Member",

        email: user.email,

        phone: "",

        age: null,

        gender: "",

        joinDate: new Date().toISOString().slice(0, 10),

        active: true,

      }),

    });

  }

  async function submitAuth(event) {

    event.preventDefault();

    setLoading(true);

    setError("");

    setMessage("");

    try {

      const result =

        mode === "register"

          ? await api("/auth/register", {

              method: "POST",

              body: JSON.stringify(form),

            })

          : await api("/auth/login", {

              method: "POST",

              body: JSON.stringify({

                email: form.email,

                password: form.password,

              }),

            });

      const saved = {

        id: result.id,

        fullName: result.fullName || "",

        email: result.email,

        role: result.role || "USER",

      };

      localStorage.setItem(ACCOUNT_KEY, JSON.stringify(saved));

      setAccount(saved);

      setForm({ fullName: "", email: "", password: "" });

      setMessage("Login successful.");

    } catch (err) {

      notifyError(err);

    } finally {

      setLoading(false);

    }

  }

  async function loadAdminData() {

    const [allMembers, allPayments, allPlans, allMemberships] =

      await Promise.all([

        api("/members"),

        api("/payments"),

        api("/membership-plans"),

        api("/member-memberships"),

      ]);

    setMembers(allMembers);

    setPayments(allPayments);

    setPlans(allPlans);

    setMemberships(allMemberships);

  }

  async function loadUserData(user) {

    const [allPlans, allMembers] = await Promise.all([

      api("/membership-plans"),

      api("/members"),

    ]);

    setPlans(allPlans.filter((p) => p.active !== false));

    let member = allMembers.find(

      (m) => m.email?.toLowerCase() === user.email?.toLowerCase()

    );

    if (!member) member = await ensureMember(user);

    setMemberRecord(member);

    setProfile({

      fullName: member.fullName || user.fullName || "",

      age: member.age == null ? "" : String(member.age),

      phone: member.phone || "",

    });

    setProfileEditing(localStorage.getItem(lockKey(user)) !== "true");

    const [userMemberships, memberPayments] = await Promise.all([

      api(`/member-memberships?memberId=${member.id}`),

      api(`/payments?memberId=${member.id}`),

    ]);

    setMemberships(userMemberships);

    setUserPayments(memberPayments);

  }

  useEffect(() => {

    if (!account) return;

    let cancelled = false;

    async function load() {

      try {

        setError("");

        if (account.role?.toUpperCase() === "ADMIN") {

          await loadAdminData();

        } else {

          await loadUserData(account);

        }

      } catch (err) {

        if (!cancelled) notifyError(err);

      }

    }

    load();

    return () => {

      cancelled = true;

    };

  }, [account?.id, account?.email, account?.role]);

  // Refresh payment and membership statuses without overwriting profile fields

  // while the user is editing their profile.

  useEffect(() => {

    if (!account || account.role?.toUpperCase() === "ADMIN") return;

    let cancelled = false;

    const refreshStatusOnly = async () => {

      try {

        const allMembers = await api("/members");

        const member = allMembers.find(

          (m) => m.email?.toLowerCase() === account.email?.toLowerCase()

        );

        if (!member || cancelled) return;

        const [userMemberships, memberPayments] = await Promise.all([

          api(`/member-memberships?memberId=${member.id}`),

          api(`/payments?memberId=${member.id}`),

        ]);

        if (cancelled) return;

        setMemberRecord(member);

        setMemberships(userMemberships);

        setUserPayments(memberPayments);

      } catch {

        // Keep the current screen usable and retry on the next interval.

      }

    };

    const timerId = window.setInterval(refreshStatusOnly, 5000);

    return () => {

      cancelled = true;

      window.clearInterval(timerId);

    };

  }, [account?.id, account?.email, account?.role]);

  async function refreshData() {

    try {

      setError("");

      if (isAdmin) await loadAdminData();

      else await loadUserData(account);

      setMessage("Data refreshed.");

    } catch (err) {

      notifyError(err);

    }

  }

  async function toggleMember(member) {

    try {

      await api(`/members/${member.id}`, {

        method: "PUT",

        body: JSON.stringify({ ...member, active: member.active === false }),

      });

      await loadAdminData();

      setMessage("Member status updated.");

    } catch (err) {

      notifyError(err);

    }

  }

  function startEditMember(member) {

    setMemberEdits((old) => ({

      ...old,

      [member.id]: {

        fullName: member.fullName || "",

        email: member.email || "",

        phone: member.phone || "",

        age: member.age == null ? "" : String(member.age),

        gender: member.gender || "",

        heightCm: member.heightCm == null ? "" : String(member.heightCm),

        weightKg: member.weightKg == null ? "" : String(member.weightKg),

        fitnessGoal: member.fitnessGoal || "",

        joinDate: member.joinDate || "",

      },

    }));

  }

  function changeMemberEdit(memberId, field, value) {

    setMemberEdits((old) => ({

      ...old,

      [memberId]: { ...(old[memberId] || {}), [field]: value },

    }));

  }

  async function saveMemberEdit(member) {

    const edit = memberEdits[member.id];

    if (!edit) return;

    if (!edit.fullName.trim() || !edit.email.trim()) {

      setError("Member name and email are required.");

      setMessage("");

      return;

    }

    const age = edit.age === "" ? null : Number(edit.age);

    const heightCm = edit.heightCm === "" ? null : Number(edit.heightCm);

    const weightKg = edit.weightKg === "" ? null : Number(edit.weightKg);

    if (age !== null && (!Number.isInteger(age) || age < 1 || age > 120)) {

      setError("Age must be between 1 and 120, or leave it blank.");

      setMessage("");

      return;

    }

    if (heightCm !== null && (!Number.isFinite(heightCm) || heightCm <= 0)) {

      setError("Height must be a positive number or left blank.");

      setMessage("");

      return;

    }

    if (weightKg !== null && (!Number.isFinite(weightKg) || weightKg <= 0)) {

      setError("Weight must be a positive number or left blank.");

      setMessage("");

      return;

    }

    setSavingMember((old) => ({ ...old, [member.id]: true }));

    setError("");

    setMessage("");

    try {

      await api(`/members/${member.id}`, {

        method: "PUT",

        body: JSON.stringify({

          ...member,

          fullName: edit.fullName.trim(),

          email: edit.email.trim(),

          phone: edit.phone.trim(),

          age,

          gender: edit.gender,

          heightCm,

          weightKg,

          fitnessGoal: edit.fitnessGoal.trim(),

          joinDate: edit.joinDate || null,

          active: member.active !== false,

        }),

      });

      setMemberEdits((old) => {

        const next = { ...old };

        delete next[member.id];

        return next;

      });

      await loadAdminData();

      setMessage(`Profile details for ${edit.fullName.trim()} saved to the database.`);

    } catch (err) {

      notifyError(err);

    } finally {

      setSavingMember((old) => ({ ...old, [member.id]: false }));

    }

  }

  function changePayment(payment, field, value) {

    setPaymentEdits((old) => ({

      ...old,

      [payment.id]: {

        ...(old[payment.id] || {}),

        [field]: value,

      },

    }));

  }

  async function savePayment(payment) {

    setSavingPayment((old) => ({ ...old, [payment.id]: true }));

    setError("");

    setMessage("");

    try {

      const changes = paymentEdits[payment.id] || {};

      await api(`/payments/${payment.id}`, {

        method: "PUT",

        body: JSON.stringify({ ...payment, ...changes }),

      });

      setPaymentEdits((old) => {

        const next = { ...old };

        delete next[payment.id];

        return next;

      });

      setPaymentEditMode((old) => ({ ...old, [payment.id]: false }));

      await loadAdminData();

      setMessage(`Payment #${payment.id} saved successfully.`);

    } catch (err) {

      notifyError(err);

    } finally {

      setSavingPayment((old) => ({ ...old, [payment.id]: false }));

    }

  }

  function updateNewPayment(memberId, field, value) {

    setNewPayments((old) => ({

      ...old,

      [memberId]: {

        method: "OFFLINE",

        status: "PENDING",

        amount: "",

        reference: "",

        ...(old[memberId] || {}),

        [field]: value,

      },

    }));

  }

  async function createMemberPayment(member) {

    const draft = newPayments[member.id] || {};

    const amount = Number(draft.amount);

    if (!Number.isFinite(amount) || amount <= 0) {

      setError(`Enter a valid positive amount for ${member.fullName}.`);

      return;

    }

    setSavingPayment((old) => ({ ...old, [`new-${member.id}`]: true }));

    setError("");

    setMessage("");

    try {

      await api("/payments", {

        method: "POST",

        body: JSON.stringify({

          memberId: member.id,

          amount,

          method: draft.method || "OFFLINE",

          status: draft.status || "PENDING",

          paymentDate: new Date().toISOString().slice(0, 10),

          reference:

            draft.reference?.trim() ||

            `ADMIN-${member.id}-${Date.now()}`,

        }),

      });

      await loadAdminData();

      setNewPayments((old) => {

        const next = { ...old };

        delete next[member.id];

        return next;

      });

      setMessage(`Payment record created for ${member.fullName}.`);

    } catch (err) {

      notifyError(err);

    } finally {

      setSavingPayment((old) => ({

        ...old,

        [`new-${member.id}`]: false,

      }));

    }

  }

  async function saveProfile(event) {

    event.preventDefault();

    setError("");

    setMessage("");

    if (!memberRecord?.id) {

      setError("Member record not found. Refresh and try again.");

      return;

    }

    const age = Number(profile.age);

    if (!profile.fullName.trim()) {

      setError("Enter your name.");

      return;

    }

    if (!profile.age || !Number.isInteger(age) || age < 1 || age > 120) {

      setError("Enter a valid age between 1 and 120.");

      return;

    }

    if (!/^[0-9+\-\s()]{7,20}$/.test(profile.phone.trim())) {

      setError("Enter a valid phone number.");

      return;

    }

    setLoading(true);

    try {

      const updated = await api(`/members/${memberRecord.id}`, {

        method: "PUT",

        body: JSON.stringify({

          ...memberRecord,

          fullName: profile.fullName.trim(),

          age,

          phone: profile.phone.trim(),

        }),

      });

      setMemberRecord(updated);

      setProfile({

        fullName: updated.fullName || "",

        age: updated.age == null ? "" : String(updated.age),

        phone: updated.phone || "",

      });

      const nextAccount = { ...account, fullName: updated.fullName };

      localStorage.setItem(ACCOUNT_KEY, JSON.stringify(nextAccount));

      localStorage.setItem(lockKey(nextAccount), "true");

      setAccount(nextAccount);

      setProfileEditing(false);

      setMessage("Profile saved and locked.");

    } catch (err) {

      notifyError(err);

    } finally {

      setLoading(false);

    }

  }

  async function choosePlan(planId) {

    const hasBlocking = memberships.some((m) =>

      ["ACTIVE", "PENDING_PAYMENT"].includes(String(m.status || "").toUpperCase())

    );

    if (hasBlocking) {

      setError("You already have an active or pending membership. You cannot select another plan yet.");

      setMessage("");

      return;

    }

    if (planSubmitting) return;

    try {

      setPlanSubmitting(true);

      setError("");

      setMessage("");

      const member = memberRecord || (await ensureMember(account));

      await api("/member-memberships", {

        method: "POST",

        body: JSON.stringify({ memberId: member.id, planId: Number(planId) }),

      });

      await loadUserData(account);

      setMessage("Plan selected. Your membership is pending payment.");

    } catch (err) {

      notifyError(err);

      try { await loadUserData(account); } catch { /* preserve original error */ }

    } finally {

      setPlanSubmitting(false);

    }

  }

  function logout() {

    localStorage.removeItem(ACCOUNT_KEY);

    setAccount(null);

    setMembers([]);

    setPayments([]);

    setUserPayments([]);

    setPlans([]);

    setMemberships([]);

    setMemberRecord(null);

    setError("");

    setMessage("");

  }

  const memberById = Object.fromEntries(

    members.map((member) => [Number(member.id), member])

  );

  const paymentMemberIds = new Set(payments.map((p) => Number(p.memberId)));

  const membersWithoutPayments = members.filter(

    (m) => !paymentMemberIds.has(Number(m.id))

  );

  const currentMembership =

    memberships.find((m) => ["ACTIVE", "PENDING_PAYMENT"].includes(String(m.status || "").toUpperCase()))

    || memberships[0] || null;

  const hasBlockingMembership = memberships.some((m) =>

    ["ACTIVE", "PENDING_PAYMENT"].includes(String(m.status || "").toUpperCase())

  );

  function readableMembershipStatus(status) {

    const labels = {

      ACTIVE: "Active",

      PENDING_PAYMENT: "Payment Pending",

      PAYMENT_FAILED: "Payment Failed",

      FAILED: "Payment Failed",

      CANCELLED: "Cancelled",

      EXPIRED: "Expired",

      REFUNDED: "Refunded",

      SUPERSEDED: "Replaced by a newer membership",

    };

    return labels[String(status || "").toUpperCase()] || status || "Unknown";

  }

  function readablePaymentStatus(status) {

    const value = String(status || "PENDING").toUpperCase();

    const labels = { PENDING: "Payment Pending", COMPLETED: "Payment Completed", FAILED: "Payment Failed", REFUNDED: "Refunded" };

    return labels[value] || value;

  }

  if (!account) {

    return (

      <main style={styles.page}>

        <div style={{ ...styles.container, maxWidth: "460px" }}>

          <header style={{ textAlign: "center", margin: "55px 0 30px" }}>

            <h1 style={{ color: "#23834a", letterSpacing: "3px" }}>IRONCORE</h1>

            <p style={styles.muted}>GYM MANAGEMENT SYSTEM</p>

          </header>

          <section style={styles.panel}>

            <div style={{ display: "flex", gap: "8px" }}>

              <button

                style={mode === "login" ? styles.button : styles.secondary}

                onClick={() => setMode("login")}

              >

                Login

              </button>

              <button

                style={mode === "register" ? styles.button : styles.secondary}

                onClick={() => setMode("register")}

              >

                Register

              </button>

            </div>

            <h2>{mode === "login" ? "Welcome back" : "Create account"}</h2>

            <form onSubmit={submitAuth}>

              {mode === "register" && (

                <Field

                  label="Full name"

                  name="fullName"

                  value={form.fullName}

                  onChange={(e) =>

                    setForm({ ...form, fullName: e.target.value })

                  }

                  required

                />

              )}

              <Field

                label="Email"

                type="email"

                value={form.email}

                onChange={(e) => setForm({ ...form, email: e.target.value })}

                required

              />

              <Field

                label="Password"

                type="password"

                value={form.password}

                onChange={(e) => setForm({ ...form, password: e.target.value })}

                minLength={8}

                required

              />

              <button style={{ ...styles.button, width: "100%" }} disabled={loading}>

                {loading ? "Please wait..." : mode === "login" ? "Login" : "Register"}

              </button>

            </form>

            {message && <p style={{ color: "#23834a" }}>{message}</p>}

            {error && <p style={{ color: "#c62828" }}>{error}</p>}

          </section>

        </div>

      </main>

    );

  }

  return (

    <main style={styles.page}>

      <div style={styles.container}>

        <header

          style={{

            display: "flex",

            justifyContent: "space-between",

            alignItems: "center",

            flexWrap: "wrap",

            gap: "12px",

            marginBottom: "24px",

          }}

        >

          <div>

            <h1 style={{ color: "#23834a", marginBottom: "6px" }}>IRONCORE</h1>

            <span style={styles.muted}>

              {isAdmin ? "Admin Dashboard" : "Member Dashboard"}

            </span>

          </div>

          <div>

            <strong>{account.fullName || account.email}</strong>

            <button style={styles.secondary} onClick={refreshData}>Refresh</button>

            <button style={styles.button} onClick={logout}>Logout</button>

          </div>

        </header>

        {message && <section style={{ ...styles.panel, color: "#23834a" }}>{message}</section>}

        {error && <section style={{ ...styles.panel, color: "#c62828" }}>{error}</section>}

        {isAdmin ? (

          <>

            <section style={styles.panel}>

              <h2>Members</h2>

              <p style={styles.muted}>Total members: {members.length}</p>

              <div style={styles.tableWrap}>

                <table style={styles.table}>

                  <thead>

                    <tr>

                      {["ID", "Name", "Email", "Phone", "Age", "Status", "Action"].map((h) => (

                        <th key={h} style={styles.cell}>{h}</th>

                      ))}

                    </tr>

                  </thead>

                  <tbody>

                    {members.map((m) => {

                      const edit = memberEdits[m.id];

                      const field = (key, label, type = "text") => (

                        <label key={key} style={{ display: "block", minWidth: "180px", fontSize: "12px" }}>

                          {label}

                          <input

                            style={styles.input}

                            type={type}

                            value={edit?.[key] ?? ""}

                            onChange={(event) => changeMemberEdit(m.id, key, event.target.value)}

                          />

                        </label>

                      );

                      return (

                        <>

                          <tr key={m.id}>

                            <td style={styles.cell}>{m.id}</td>

                            <td style={styles.cell}>{m.fullName}</td>

                            <td style={styles.cell}>{m.email}</td>

                            <td style={styles.cell}>{m.phone || "—"}</td>

                            <td style={styles.cell}>{m.age ?? "—"}</td>

                            <td style={styles.cell}>{m.active === false ? "Inactive" : "Active"}</td>

                            <td style={styles.cell}>

                              <button style={styles.secondary} onClick={() => edit ? setMemberEdits((old) => { const next = { ...old }; delete next[m.id]; return next; }) : startEditMember(m)}>

                                {edit ? "Cancel Edit" : "Edit Profile"}

                              </button>

                              <button style={styles.secondary} onClick={() => toggleMember(m)}>

                                {m.active === false ? "Activate" : "Deactivate"}

                              </button>

                            </td>

                          </tr>

                          {edit && (

                            <tr key={`edit-${m.id}`}>

                              <td style={styles.cell} colSpan={7}>

                                <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))", gap: "10px", padding: "12px", background: "#f7faf7", borderRadius: "8px" }}>

                                  {field("fullName", "Full name *")}

                                  {field("email", "Email *", "email")}

                                  {field("phone", "Phone number", "tel")}

                                  {field("age", "Age", "number")}

                                  {field("gender", "Gender")}

                                  {field("heightCm", "Height (cm)", "number")}

                                  {field("weightKg", "Weight (kg)", "number")}

                                  {field("fitnessGoal", "Fitness goal")}

                                  {field("joinDate", "Join date", "date")}

                                </div>

                                <button style={styles.button} disabled={savingMember[m.id]} onClick={() => saveMemberEdit(m)}>

                                  {savingMember[m.id] ? "Saving..." : "Save Profile Details"}

                                </button>

                                <span style={styles.muted}> Changes are saved to the backend and MySQL.</span>

                              </td>

                            </tr>

                          )}

                        </>

                      );

                    })}

                    {!members.length && (

                      <tr><td style={styles.cell} colSpan={7}>No members found.</td></tr>

                    )}

                  </tbody>

                </table>

              </div>

            </section>

            <section style={styles.panel}>

              <h2>Payments — All Members</h2>

              <p style={styles.muted}>

                Select the method and status, then click Save Changes. Members without a payment record are listed below.

              </p>

              <div style={styles.tableWrap}>

                <table style={styles.table}>

                  <thead>

                    <tr>

                      {["Payment ID", "Member", "Email", "Amount (₹)", "Date", "Payment Method", "Payment Status", "Reference", "Action"].map((h) => (

                        <th key={h} style={styles.cell}>{h}</th>

                      ))}

                    </tr>

                  </thead>

                  <tbody>

                    {payments.map((payment) => {

                      const member = memberById[Number(payment.memberId)];

                      const edit = paymentEdits[payment.id] || {};

                      const method = edit.method ?? payment.method ?? "ONLINE";

                      const status = edit.status ?? payment.status ?? "PENDING";

                      const reference = edit.reference ?? payment.reference ?? "";

                      const dirty =
                        method !== (payment.method ?? "ONLINE") ||
                        status !== (payment.status ?? "PENDING") ||
                        reference !== (payment.reference ?? "");

                      const isEditingPayment = paymentEditMode[payment.id] === true;

                      return (

                        <tr key={payment.id}>

                          <td style={styles.cell}>#{payment.id}</td>

                          <td style={styles.cell}>

                            <strong>{member?.fullName || "Member not found"}</strong>

                            <div style={styles.muted}>ID: {payment.memberId}</div>

                          </td>

                          <td style={styles.cell}>{member?.email || "—"}</td>

                          <td style={styles.cell}>₹{payment.amount}</td>

                          <td style={styles.cell}>{payment.paymentDate || "—"}</td>

                          <td style={styles.cell}>

                            {isEditingPayment ? (

                              <select

                                style={styles.input}

                                value={method}

                                disabled={savingPayment[payment.id]}

                                onChange={(e) => changePayment(payment, "method", e.target.value)}

                              >

                                <option value="ONLINE">Online</option>

                                <option value="OFFLINE">Offline / Cash</option>

                              </select>

                            ) : (

                              String(payment.method || "ONLINE").toUpperCase() === "OFFLINE" ? "Offline / Cash" : "Online"

                            )}

                          </td>

                          <td style={styles.cell}>

                            {isEditingPayment ? (

                              <select

                                style={styles.input}

                                value={status}

                                disabled={savingPayment[payment.id]}

                                onChange={(e) => changePayment(payment, "status", e.target.value)}

                              >

                                <option value="PENDING">Pending</option>

                                <option value="COMPLETED">Completed</option>

                                <option value="FAILED">Failed</option>

                                <option value="REFUNDED">Refunded</option>

                              </select>

                            ) : (

                              status

                            )}

                          </td>

                          <td style={styles.cell}>
                            {isEditingPayment ? (
                              <input
                                type="text"
                                style={{ ...styles.input, minWidth: "160px" }}
                                placeholder="Enter payment reference"
                                value={reference}
                                onChange={(e) => changePayment(payment, "reference", e.target.value)}
                                disabled={savingPayment[payment.id]}
                              />
                            ) : (
                              payment.reference || "—"
                            )}
                          </td>

                          <td style={styles.cell}>

                            {!isEditingPayment ? (

                              <button

                                type="button"

                                style={styles.secondary}

                                onClick={() => {

                                  setError("");

                                  setMessage("");

                                  setPaymentEdits((old) => ({

                                    ...old,

                                    [payment.id]: {

                                      method: payment.method || "ONLINE",

                                      status: payment.status || "PENDING",

                                      reference: payment.reference || "",

                                    },

                                  }));

                                  setPaymentEditMode((old) => ({ ...old, [payment.id]: true }));

                                }}

                              >

                                Edit Payment

                              </button>

                            ) : (

                              <>

                                <button

                                  type="button"

                                  style={styles.button}

                                  disabled={!dirty || savingPayment[payment.id]}

                                  onClick={() => savePayment(payment)}

                                >

                                  {savingPayment[payment.id] ? "Saving..." : "Save Changes"}

                                </button>

                                <button

                                  type="button"

                                  style={styles.secondary}

                                  disabled={savingPayment[payment.id]}

                                  onClick={() => {

                                    setPaymentEdits((old) => {

                                      const next = { ...old };

                                      delete next[payment.id];

                                      return next;

                                    });

                                    setPaymentEditMode((old) => ({ ...old, [payment.id]: false }));

                                  }}

                                >

                                  Cancel

                                </button>

                              </>

                            )}

                          </td>

                        </tr>

                      );

                    })}

                    {membersWithoutPayments.map((member) => {

                      const draft = newPayments[member.id] || {

                        amount: "",

                        method: "OFFLINE",

                        status: "PENDING",

                        reference: "",

                      };

                      const busy = savingPayment[`new-${member.id}`];

                      return (

                        <tr key={`no-payment-${member.id}`}>

                          <td style={styles.cell}>—</td>

                          <td style={styles.cell}>

                            <strong>{member.fullName}</strong>

                            <div style={styles.muted}>ID: {member.id}</div>

                          </td>

                          <td style={styles.cell}>{member.email}</td>

                          <td style={styles.cell}>

                            <input

                              style={{ ...styles.input, minWidth: "100px" }}

                              type="number"

                              min="1"

                              step="0.01"

                              placeholder="Amount"

                              value={draft.amount}

                              onChange={(e) => updateNewPayment(member.id, "amount", e.target.value)}

                            />

                          </td>

                          <td style={styles.cell}>Not recorded</td>

                          <td style={styles.cell}>

                            <select

                              style={styles.input}

                              value={draft.method}

                              onChange={(e) => updateNewPayment(member.id, "method", e.target.value)}

                            >

                              <option value="ONLINE">Online</option>

                              <option value="OFFLINE">Offline / Cash</option>

                            </select>

                          </td>

                          <td style={styles.cell}>

                            <select

                              style={styles.input}

                              value={draft.status}

                              onChange={(e) => updateNewPayment(member.id, "status", e.target.value)}

                            >

                              <option value="PENDING">Pending</option>

                              <option value="COMPLETED">Completed</option>

                              <option value="FAILED">Failed</option>

                              <option value="REFUNDED">Refunded</option>

                            </select>

                          </td>

                          <td style={styles.cell}>

                            <input

                              style={styles.input}

                              placeholder="Reference (optional)"

                              value={draft.reference}

                              onChange={(e) => updateNewPayment(member.id, "reference", e.target.value)}

                            />

                          </td>

                          <td style={styles.cell}>

                            <button

                              style={styles.button}

                              disabled={busy}

                              onClick={() => createMemberPayment(member)}

                            >

                              {busy ? "Saving..." : "Create Payment"}

                            </button>

                          </td>

                        </tr>

                      );

                    })}

                    {payments.length === 0 && members.length === 0 && (

                      <tr><td style={styles.cell} colSpan={9}>No members or payment records found.</td></tr>

                    )}

                  </tbody>

                </table>

              </div>

            </section>

            <section style={styles.panel}>

              <h2>Membership Plans</h2>

              {plans.map((plan) => (

                <div key={plan.id} style={{ borderTop: "1px solid #e5eae6", padding: "12px 0" }}>

                  <strong>{plan.name}</strong> — ₹{plan.price} / {plan.durationMonths} month(s)

                  <p style={styles.muted}>{plan.description}</p>

                </div>

              ))}

            </section>

          </>

        ) : (

          <>

            <section style={styles.panel}>

              <h2>My Profile</h2>

              <p style={styles.muted}>Save your details to lock them. Use Edit Profile to change them later.</p>

              <form onSubmit={saveProfile}>

                <label style={{ display: "block" }}>

                  Name

                  <input

                    style={styles.input}

                    value={profile.fullName}

                    disabled={!profileEditing}

                    required

                    onChange={(e) => setProfile({ ...profile, fullName: e.target.value })}

                  />

                </label>

                <label style={{ display: "block" }}>

                  Age

                  <input

                    style={styles.input}

                    type="number"

                    min="1"

                    max="120"

                    value={profile.age}

                    disabled={!profileEditing}

                    required

                    onChange={(e) => setProfile({ ...profile, age: e.target.value })}

                  />

                </label>

                <label style={{ display: "block" }}>

                  Phone Number

                  <input

                    style={styles.input}

                    type="tel"

                    value={profile.phone}

                    disabled={!profileEditing}

                    required

                    onChange={(e) => setProfile({ ...profile, phone: e.target.value })}

                  />

                </label>

                {profileEditing ? (

                  <button style={styles.button} disabled={loading}>

                    {loading ? "Saving..." : "Save Profile"}

                  </button>

                ) : (

                  <button

                    type="button"

                    style={styles.button}

                    onClick={() => {

                      setError("");

                      setMessage("");

                      setProfile({

                        fullName: memberRecord?.fullName || account.fullName || "",

                        age: memberRecord?.age == null ? "" : String(memberRecord.age),

                        phone: memberRecord?.phone || "",

                      });

                      localStorage.removeItem(lockKey(account));

                      setProfileEditing(true);

                    }}

                  >

                    Edit Profile

                  </button>

                )}

              </form>

            </section>

            <section style={styles.panel}>

              <h2>Choose Your Membership Plan</h2>

              {hasBlockingMembership && currentMembership && (

                <div style={{ border: "1px solid #b8d9c0", background: "#f0faf2", borderRadius: "8px", padding: "14px", marginBottom: "16px" }}>

                  <strong>Current membership: {plans.find((p) => Number(p.id) === Number(currentMembership.planId))?.name || `Plan #${currentMembership.planId}`}</strong>

                  <p style={{ marginBottom: 0 }}>Status: {readableMembershipStatus(currentMembership.status)}. Only one active or pending membership is allowed.</p>

                </div>

              )}

              <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit,minmax(220px,1fr))", gap: "16px" }}>

                {plans.map((plan) => {

                  const isCurrentPlan = currentMembership && Number(currentMembership.planId) === Number(plan.id) && hasBlockingMembership;

                  return (

                    <article key={plan.id} style={{ border: "1px solid #e0e7e1", borderRadius: "10px", padding: "18px" }}>

                      <h3>{plan.name}</h3>

                      <h2 style={{ color: "#23834a" }}>₹{plan.price}</h2>

                      <p>{plan.durationMonths} month(s)</p>

                      <p style={styles.muted}>{plan.description}</p>

                      <button

                        style={{ ...(isCurrentPlan ? styles.secondary : styles.button), opacity: hasBlockingMembership && !isCurrentPlan ? 0.55 : 1, cursor: hasBlockingMembership || planSubmitting ? "not-allowed" : "pointer" }}

                        disabled={hasBlockingMembership || planSubmitting}

                        onClick={() => choosePlan(plan.id)}

                      >

                        {isCurrentPlan ? readableMembershipStatus(currentMembership.status) : hasBlockingMembership ? "Unavailable — Current Plan Active" : planSubmitting ? "Saving..." : "Select Plan"}

                      </button>

                    </article>

                  );

                })}

              </div>

              {hasBlockingMembership && <p style={styles.muted}>Other plans become selectable when your current membership is no longer active or pending.</p>}

            </section>

            <section style={styles.panel}>

              <h2>My Payments</h2>

              {userPayments.length ? (

                <div style={styles.tableWrap}>

                  <table style={{ ...styles.table, minWidth: "650px" }}>

                    <thead><tr>{["Payment ID", "Amount", "Date", "Method", "Payment Status", "Reference"].map((heading) => <th key={heading} style={styles.cell}>{heading}</th>)}</tr></thead>

                    <tbody>

                      {userPayments.slice().sort((a, b) => Number(b.id) - Number(a.id)).map((payment) => (

                        <tr key={payment.id}>

                          <td style={styles.cell}>#{payment.id}</td>

                          <td style={styles.cell}>₹{payment.amount}</td>

                          <td style={styles.cell}>{payment.paymentDate || "—"}</td>

                          <td style={styles.cell}>{String(payment.method || "—").toUpperCase() === "OFFLINE" ? "Offline / Cash" : String(payment.method || "—").toUpperCase() === "ONLINE" ? "Online" : payment.method || "—"}</td>

                          <td style={styles.cell}><strong>{readablePaymentStatus(payment.status)}</strong></td>

                          <td style={styles.cell}>{payment.reference || "—"}</td>

                        </tr>

                      ))}

                    </tbody>

                  </table>

                </div>

              ) : (

                <p style={styles.muted}>No payment record yet. Your payment status will appear here after the administrator records or updates your payment.</p>

              )}

              <button type="button" style={styles.secondary} onClick={refreshData}>Refresh Payment Status</button>

            </section>

            <section style={styles.panel}>

              <h2>My Membership</h2>

              {currentMembership ? (

                <div style={{ border: "1px solid #e0e7e1", borderRadius: "8px", padding: "16px" }}>

                  <h3 style={{ marginTop: 0 }}>{plans.find((p) => Number(p.id) === Number(currentMembership.planId))?.name || `Plan #${currentMembership.planId}`}</h3>

                  <p><strong>Membership ID:</strong> {currentMembership.id}</p>

                  <p><strong>Start Date:</strong> {currentMembership.startDate || "—"}</p>

                  <p><strong>End Date:</strong> {currentMembership.endDate || "—"}</p>

                  <p><strong>Status:</strong> {readableMembershipStatus(currentMembership.status)}</p>

                  <button type="button" style={styles.secondary} onClick={refreshData}>Refresh Membership Status</button>

                  <p style={styles.muted}>This status refreshes automatically after admin payment updates.</p>

                </div>

              ) : (

                <p style={styles.muted}>No membership selected yet.</p>

              )}

            </section>

          </>

        )}

        <footer style={{ ...styles.muted, textAlign: "center", padding: "20px" }}>

          IRONCORE Gym Management System

        </footer>

      </div>

    </main>

  );

}
