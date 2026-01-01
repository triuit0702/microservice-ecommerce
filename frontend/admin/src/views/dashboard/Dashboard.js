// views/dashboard/Dashboard.jsx
import { useEffect, useState } from "react";
import { CRow, CCol } from "@coreui/react";

//import { getDashboard } from "./dashboardService";
import StatCard from "./StatCard";
import LoginChart from "./LoginChart";
import { serviceGetDashboard } from "../../services/DashboardService";
import RecentActivity from "./RecentActivity";

const Dashboard = () => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    serviceGetDashboard()
      .then((res) => setData(res.data.data))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return null;
  if (!data) return null;

  return (
    <>
      {/* Stats */}
      <CRow>
        <CCol md={4}>
          <StatCard title="Total Users" value={data.totalUsers} />
        </CCol>
        <CCol md={4}>
          <StatCard title="Login Today" value={data.loginToday} />
        </CCol>
        <CCol md={4}>
          <StatCard title="Online Users" value={data.onlineUsers} />
        </CCol>
      </CRow>

      {/* Chart */}
      <CRow>
        <CCol>
          <LoginChart data={data.loginChart} />
        </CCol>
      </CRow>

      <RecentActivity />
    </>
  );
};

export default Dashboard;
