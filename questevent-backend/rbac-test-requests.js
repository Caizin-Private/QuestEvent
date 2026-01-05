// QuestEvent RBAC Browser Test Requests

// ==================== PROGRAM ENDPOINTS ====================

// Create Program (Owner/Host)
(() => {
  return fetch("/api/programs", {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      hostUserId: 6,
      programTitle: "RBAC Test Program",
      programDescription: "Testing RBAC functionality",
      department: "IT",
      startDate: "2026-01-01T00:00:00",
      endDate: "2026-12-31T23:59:59",
      registrationFee: 0,
      status: "ACTIVE"
    })
  });
})()
.then(async res => {
  console.log("Create Program Status:", res.status);
  const text = await res.text();
  console.log("Program Response:", text);
})
.catch(console.error);

// Get All Programs (Any authenticated user)
(() => {
  return fetch("/api/programs", {
    method: "GET",
    credentials: "include"
  });
})()
.then(async res => {
  console.log("Get All Programs Status:", res.status);
  const text = await res.text();
  console.log("Programs Response:", text);
})
.catch(console.error);

// Get Program by ID (Any authenticated user)
(() => {
  return fetch("/api/programs/20", {
    method: "GET",
    credentials: "include"
  });
})()
.then(async res => {
  console.log("Get Program Status:", res.status);
  const text = await res.text();
  console.log("Program Response:", text);
})
.catch(console.error);

// Update Program (Program owner/manager only)
(() => {
  return fetch("/api/programs/20", {
    method: "PUT",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      hostUserId: 6,
      programTitle: "Updated RBAC Test Program",
      programDescription: "Updated RBAC test description",
      department: "IT",
      startDate: "2026-01-01T00:00:00",
      endDate: "2026-12-31T23:59:59",
      registrationFee: 0,
      status: "ACTIVE"
    })
  });
})()
.then(async res => {
  console.log("Update Program Status:", res.status);
  const text = await res.text();
  console.log("Update Response:", text);
})
.catch(console.error);

// Delete Program (Program owner only)
(() => {
  return fetch("/api/programs/20?userId=6", {
    method: "DELETE",
    credentials: "include"
  });
})()
.then(async res => {
  console.log("Delete Program Status:", res.status);
  const text = await res.text();
  console.log("Delete Response:", text);
})
.catch(console.error);

// ==================== ACTIVITY ENDPOINTS ====================

// Create Activity (Program manager only)
(() => {
  return fetch("/api/programs/21/activities", {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      activityName: "RBAC Test Activity",
      activityDescription: "Testing RBAC activity functionality",
      activityDuration: 120,
      activityRulebook: "Standard RBAC test rules",
      rewardGems: 100,
      isCompulsory: false
    })
  });
})()
.then(async res => {
  console.log("Create Activity Status:", res.status);
  const text = await res.text();
  console.log("Activity Response:", text);
})
.catch(console.error);

// Get Activities for Program (Program participants)
(() => {
  return fetch("/api/programs/21/activities", {
    method: "GET",
    credentials: "include"
  });
})()
.then(async res => {
  console.log("Get Activities Status:", res.status);
  const text = await res.text();
  console.log("Activities Response:", text);
})
.catch(console.error);

// Update Activity (Program manager only)
(() => {
  return fetch("/api/programs/21/activities/13", {
    method: "PUT",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      activityName: "Updated RBAC Test Activity",
      activityDescription: "Updated RBAC activity description",
      activityDuration: 150,
      activityRulebook: "Updated RBAC test rules",
      rewardGems: 120,
      isCompulsory: false
    })
  });
})()
.then(async res => {
  console.log("Update Activity Status:", res.status);
  const text = await res.text();
  console.log("Update Activity Response:", text);
})
.catch(console.error);

// Delete Activity (Program manager only)
(() => {
  return fetch("/api/programs/21/activities/13", {
    method: "DELETE",
    credentials: "include"
  });
})()
.then(async res => {
  console.log("Delete Activity Status:", res.status);
  const text = await res.text();
  console.log("Delete Activity Response:", text);
})
.catch(console.error);

// ==================== REGISTRATION ENDPOINTS ====================

// Register for Program
(() => {
  return fetch("/api/program-registrations", {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      userId: 6,
      programId: 21
    })
  });
})()
.then(async res => {
  console.log("Program Registration Status:", res.status);
  const text = await res.text();
  console.log("Program Registration Response:", text);
})
.catch(console.error);

// Register for Activity
(() => {
  return fetch("/api/activity-registrations", {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      userId: 6,
      activityId: 13
    })
  });
})()
.then(async res => {
  console.log("Activity Registration Status:", res.status);
  const text = await res.text();
  console.log("Activity Registration Response:", text);
})
.catch(console.error);

// Get All Program Registrations (Owner/Judge only)
(() => {
  return fetch("/api/program-registrations", {
    method: "GET",
    credentials: "include"
  });
})()
.then(async res => {
  console.log("Get All Program Registrations Status:", res.status);
  const text = await res.text();
  console.log("All Program Registrations Response:", text);
})
.catch(console.error);

// Get Program Registrations by User
(() => {
  return fetch("/api/program-registrations/users/6", {
    method: "GET",
    credentials: "include"
  });
})()
.then(async res => {
  console.log("User Program Registrations Status:", res.status);
  const text = await res.text();
  console.log("User Program Registrations Response:", text);
})
.catch(console.error);

// Get Activity Registrations by User
(() => {
  return fetch("/api/activity-registrations/users/6", {
    method: "GET",
    credentials: "include"
  });
})()
.then(async res => {
  console.log("User Activity Registrations Status:", res.status);
  const text = await res.text();
  console.log("User Activity Registrations Response:", text);
})
.catch(console.error);

// ==================== SUBMISSION ENDPOINTS ====================

// Submit Activity
(() => {
  return fetch("/api/submissions", {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      activityId: 12,
      userId: 6,
      submissionUrl: "https://github.com/rbac-test/submission"
    })
  });
})()
.then(async res => {
  console.log("Submit Activity Status:", res.status);
  const text = await res.text();
  console.log("Submission Response:", text);
})
.catch(console.error);

// ==================== JUDGE ENDPOINTS ====================

// Get Pending Submissions (Judge/Owner only)
(() => {
  return fetch("/api/judge/submissions/pending", {
    method: "GET",
    credentials: "include"
  });
})()
.then(async res => {
  console.log("Pending Submissions Status:", res.status);
  const text = await res.text();
  console.log("Pending Submissions Response:", text);
})
.catch(console.error);

// Get Submissions for Activity (Judge/Owner only)
(() => {
  return fetch("/api/judge/submissions/activity/16", {
    method: "GET",
    credentials: "include"
  });
})()
.then(async res => {
  console.log("Activity Submissions Status:", res.status);
  const text = await res.text();
  console.log("Activity Submissions Response:", text);
})
.catch(console.error);

// Review Submission (Judge only)
(() => {
  return fetch("/api/judge/review/1?judgeId=3", {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({})
  });
})()
.then(async res => {
  console.log("Review Submission Status:", res.status);
  const text = await res.text();
  console.log("Review Response:", text);
})
.catch(console.error);

// ==================== USER MANAGEMENT ENDPOINTS ====================

// Get User Profile (Self or authorized)
(() => {
  return fetch("/api/users/6", {
    method: "GET",
    credentials: "include"
  });
})()
.then(async res => {
  console.log("Get User Profile Status:", res.status);
  const text = await res.text();
  console.log("User Profile Response:", text);
})
.catch(console.error);

// Update User Profile (Self only)
(() => {
  return fetch("/api/users/6", {
    method: "PUT",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      name: "Maitreyee",
      email: "maitreyee.joshi@caizin.com",
      department: "IT",
      gender: "FEMALE"
    })
  });
})()
.then(async res => {
  console.log("Update User Profile Status:", res.status);
  const text = await res.text();
  console.log("Update User Response:", text);
})
.catch(console.error);

// Get All Users (Owner only)
(() => {
  return fetch("/api/users", {
    method: "GET",
    credentials: "include"
  });
})()
.then(async res => {
  console.log("Get All Users Status:", res.status);
  const text = await res.text();
  console.log("All Users Response:", text);
})
.catch(console.error);

// Create User (Owner only)
(() => {
  return fetch("/api/users", {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      name: "New Test User",
      email: "newuser@test.com",
      password: "password123",
      department: "IT",
      gender: "MALE",
      role: "USER"
    })
  });
})()
.then(async res => {
  console.log("Create User Status:", res.status);
  const text = await res.text();
  console.log("Create User Response:", text);
})
.catch(console.error);

// Delete User (Owner only)
(() => {
  return fetch("/api/users/999", {
    method: "DELETE",
    credentials: "include"
  });
})()
.then(async res => {
  console.log("Delete User Status:", res.status);
  const text = await res.text();
  console.log("Delete User Response:", text);
})
.catch(console.error);

// ==================== WALLET ENDPOINTS ====================

// Get User Wallet Balance (Self only)
(() => {
  return fetch("/api/users/6/wallet", {
    method: "GET",
    credentials: "include"
  });
})()
.then(async res => {
  console.log("Get User Wallet Status:", res.status);
  const text = await res.text();
  console.log("User Wallet Response:", text);
})
.catch(console.error);

// ==================== LEADERBOARD ENDPOINTS ====================

// Get Global Leaderboard (Any authenticated user)
(() => {
  return fetch("/leaderboard/global", {
    method: "GET",
    credentials: "include"
  });
})()
.then(async res => {
  console.log("Global Leaderboard Status:", res.status);
  const text = await res.text();
  console.log("Leaderboard Response:", text);
})
.catch(console.error);

// ==================== AUTH ENDPOINTS ====================

// Get Complete Profile (Any authenticated user)
(() => {
  return fetch("/complete-profile", {
    method: "GET",
    credentials: "include"
  });
})()
.then(async res => {
  console.log("Complete Profile Status:", res.status);
  const text = await res.text();
  console.log("Complete Profile Response:", text);
})
.catch(console.error);

// ==================== NEGATIVE TEST CASES ====================

// Try to access another user's wallet (Should fail - 403)
(() => {
  return fetch("/api/users/1/wallet", {
    method: "GET",
    credentials: "include"
  });
})()
.then(async res => {
  console.log("Access Other User Wallet Status:", res.status);
  const text = await res.text();
  console.log("Other User Wallet Response:", text);
})
.catch(console.error);

// Try to update program without ownership (Should fail - 403)
(() => {
  return fetch("/api/programs/19", {
    method: "PUT",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      hostUserId: 6,
      programTitle: "Unauthorized Update",
      programDescription: "This should fail",
      department: "IT",
      startDate: "2026-01-01T00:00:00",
      endDate: "2026-12-31T23:59:59",
      registrationFee: 0,
      status: "ACTIVE"
    })
  });
})()
.then(async res => {
  console.log("Unauthorized Program Update Status:", res.status);
  const text = await res.text();
  console.log("Unauthorized Update Response:", text);
})
.catch(console.error);

// Try to access judge endpoints as participant (Should fail - 403)
(() => {
  return fetch("/api/judge/submissions/pending", {
    method: "GET",
    credentials: "include"
  });
})()
.then(async res => {
  console.log("Unauthorized Judge Access Status:", res.status);
  const text = await res.text();
  console.log("Unauthorized Judge Response:", text);
})
.catch(console.error);

// Try to create user as participant (Should fail - 403)
(() => {
  return fetch("/api/users", {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      name: "Unauthorized User",
      email: "unauthorized@test.com",
      password: "password123",
      department: "IT",
      gender: "FEMALE",
      role: "PARTICIPANT"
    })
  });
})()
.then(async res => {
  console.log("Unauthorized User Creation Status:", res.status);
  const text = await res.text();
  console.log("Unauthorized User Creation Response:", text);
})
.catch(console.error);

// Try to access invalid endpoint (Should fail - 404)
(() => {
  return fetch("/api/invalid-endpoint", {
    method: "GET",
    credentials: "include"
  });
})()
.then(async res => {
  console.log("Invalid Endpoint Status:", res.status);
  const text = await res.text();
  console.log("Invalid Endpoint Response:", text);
})
.catch(console.error);

// Try to submit invalid JSON (Should fail - 400)
(() => {
  return fetch("/api/programs", {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: "invalid json"
  });
})()
.then(async res => {
  console.log("Invalid JSON Status:", res.status);
  const text = await res.text();
  console.log("Invalid JSON Response:", text);
})
.catch(console.error);

// Try to create program with missing fields (Should fail - 400)
(() => {
  return fetch("/api/programs", {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({})
  });
})()
.then(async res => {
  console.log("Missing Fields Status:", res.status);
  const text = await res.text();
  console.log("Missing Fields Response:", text);
})
.catch(console.error);

// Try to duplicate activity submission (Should fail - 400)
(() => {
  return fetch("/api/submissions", {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      activityId: 12,
      userId: 6,
      submissionUrl: "https://github.com/duplicate/submission"
    })
  });
})()
.then(async res => {
  console.log("Duplicate Submission Status:", res.status);
  const text = await res.text();
  console.log("Duplicate Submission Response:", text);
})
.catch(console.error);
