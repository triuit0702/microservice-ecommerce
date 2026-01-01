// views/dashboard/LoginChart.jsx
import { CChartLine } from "@coreui/react-chartjs";

const LoginChart = ({ data = [] }) => {
    if (data.length === 0) {
        return <p className="text-center text-muted">No data for chart</p>;
    }
    return (
        <CChartLine
            data={{
                labels: data.map((i) => i.date),
                datasets: [
                    {
                        label: "Login",
                        data: data.map((i) => i.count),
                        fill: false,
                    },
                ],
            }}
        />
    );
};

export default LoginChart;
