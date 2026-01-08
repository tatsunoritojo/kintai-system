// モックデータ - 勤怠管理システム

const mockData = {
  // ユーザー情報
  currentUser: {
    name: "山田太郎",
    email: "yamada@example.com",
    role: "ADMIN" // ADMIN or EMPLOYEE
  },

  // ダッシュボード
  dashboard: {
    monthlyStats: {
      month: "2024年1月",
      totalWorkHours: 160,
      totalWorkDays: 21,
      estimatedPayment: 240000
    },
    employeeCount: 4,
    recentWorkRecords: [
      {
        workDate: "2024-01-15",
        dayOfWeek: "月",
        startTime: "09:00",
        endTime: "18:00",
        workHours: 8.0,
        workTypeName: "個別指導"
      },
      {
        workDate: "2024-01-14",
        dayOfWeek: "日",
        startTime: "13:00",
        endTime: "20:00",
        workHours: 7.0,
        workTypeName: "自習室監督"
      },
      {
        workDate: "2024-01-13",
        dayOfWeek: "土",
        startTime: "10:00",
        endTime: "17:00",
        workHours: 7.0,
        workTypeName: "個別指導"
      },
      {
        workDate: "2024-01-12",
        dayOfWeek: "金",
        startTime: "14:00",
        endTime: "21:00",
        workHours: 7.0,
        workTypeName: "集団授業"
      },
      {
        workDate: "2024-01-11",
        dayOfWeek: "木",
        startTime: "09:00",
        endTime: "18:00",
        workHours: 8.0,
        workTypeName: "個別指導"
      }
    ],
    notifications: [
      {
        type: "success",
        message: "給与計算が完了しました",
        timestamp: "1時間前"
      },
      {
        type: "info",
        message: "Google Calendarとの同期が完了しました",
        timestamp: "3時間前"
      },
      {
        type: "warning",
        message: "未承認の勤務記録が5件あります",
        timestamp: "1日前"
      }
    ]
  },

  // 従業員一覧
  employees: [
    {
      id: 1,
      name: "山田太郎",
      email: "yamada@example.com",
      role: "ADMIN",
      status: "ACTIVE",
      hireDate: "2023-04-01",
      totalWorkHours: 160,
      totalPayment: 240000
    },
    {
      id: 2,
      name: "佐藤花子",
      email: "sato@example.com",
      role: "EMPLOYEE",
      status: "ACTIVE",
      hireDate: "2023-06-15",
      totalWorkHours: 120,
      totalPayment: 180000
    },
    {
      id: 3,
      name: "鈴木一郎",
      email: "suzuki@example.com",
      role: "EMPLOYEE",
      status: "ACTIVE",
      hireDate: "2023-08-01",
      totalWorkHours: 100,
      totalPayment: 150000
    },
    {
      id: 4,
      name: "田中美咲",
      email: "tanaka@example.com",
      role: "EMPLOYEE",
      status: "INACTIVE",
      hireDate: "2023-09-10",
      totalWorkHours: 0,
      totalPayment: 0
    }
  ],

  // 勤務記録一覧
  workRecords: [
    {
      id: 1,
      employeeName: "山田太郎",
      workDate: "2024-01-15",
      dayOfWeek: "月",
      startTime: "09:00",
      endTime: "18:00",
      workHours: 8.0,
      workTypeName: "個別指導",
      studentName: "生徒A",
      payment: 12000
    },
    {
      id: 2,
      employeeName: "山田太郎",
      workDate: "2024-01-14",
      dayOfWeek: "日",
      startTime: "13:00",
      endTime: "20:00",
      workHours: 7.0,
      workTypeName: "自習室監督",
      studentName: "-",
      payment: 10500
    },
    {
      id: 3,
      employeeName: "佐藤花子",
      workDate: "2024-01-15",
      dayOfWeek: "月",
      startTime: "14:00",
      endTime: "19:00",
      workHours: 5.0,
      workTypeName: "個別指導",
      studentName: "生徒B",
      payment: 7500
    },
    {
      id: 4,
      employeeName: "鈴木一郎",
      workDate: "2024-01-15",
      dayOfWeek: "月",
      startTime: "10:00",
      endTime: "16:00",
      workHours: 6.0,
      workTypeName: "集団授業",
      studentName: "中学3年生クラス",
      payment: 9000
    }
  ],

  // 給与計算一覧
  payrolls: [
    {
      id: 1,
      employeeName: "山田太郎",
      month: "2024年1月",
      totalWorkHours: 160,
      totalWorkDays: 21,
      totalPayment: 240000,
      status: "PAID",
      calculatedDate: "2024-01-31"
    },
    {
      id: 2,
      employeeName: "佐藤花子",
      month: "2024年1月",
      totalWorkHours: 120,
      totalWorkDays: 18,
      totalPayment: 180000,
      status: "PENDING",
      calculatedDate: "2024-01-31"
    },
    {
      id: 3,
      employeeName: "鈴木一郎",
      month: "2024年1月",
      totalWorkHours: 100,
      totalWorkDays: 15,
      totalPayment: 150000,
      status: "PENDING",
      calculatedDate: "2024-01-31"
    }
  ],

  // 単価マスタ
  hourlyWages: [
    {
      id: 1,
      workTypeName: "個別指導",
      studentLevel: "小学生",
      hourlyRate: 1500,
      validFrom: "2024-01-01",
      validTo: "2024-12-31",
      status: "ACTIVE"
    },
    {
      id: 2,
      workTypeName: "個別指導",
      studentLevel: "中学生",
      hourlyRate: 1800,
      validFrom: "2024-01-01",
      validTo: "2024-12-31",
      status: "ACTIVE"
    },
    {
      id: 3,
      workTypeName: "個別指導",
      studentLevel: "高校生",
      hourlyRate: 2000,
      validFrom: "2024-01-01",
      validTo: "2024-12-31",
      status: "ACTIVE"
    },
    {
      id: 4,
      workTypeName: "集団授業",
      studentLevel: "-",
      hourlyRate: 2500,
      validFrom: "2024-01-01",
      validTo: "2024-12-31",
      status: "ACTIVE"
    },
    {
      id: 5,
      workTypeName: "自習室監督",
      studentLevel: "-",
      hourlyRate: 1200,
      validFrom: "2024-01-01",
      validTo: "2024-12-31",
      status: "ACTIVE"
    }
  ],

  // 生徒マスタ
  students: [
    {
      id: 1,
      name: "生徒A",
      schoolType: "小学生",
      grade: "6年生",
      status: "ACTIVE",
      enrollmentDate: "2023-04-01"
    },
    {
      id: 2,
      name: "生徒B",
      schoolType: "中学生",
      grade: "2年生",
      status: "ACTIVE",
      enrollmentDate: "2023-04-01"
    },
    {
      id: 3,
      name: "生徒C",
      schoolType: "高校生",
      grade: "1年生",
      status: "ACTIVE",
      enrollmentDate: "2023-09-01"
    },
    {
      id: 4,
      name: "生徒D",
      schoolType: "中学生",
      grade: "3年生",
      status: "INACTIVE",
      enrollmentDate: "2022-04-01"
    }
  ],

  // 勤務形態マスタ
  workTypes: [
    {
      id: 1,
      name: "個別指導",
      description: "1対1または1対2の個別指導",
      requiresStudent: true,
      status: "ACTIVE"
    },
    {
      id: 2,
      name: "集団授業",
      description: "複数人のクラス授業",
      requiresStudent: false,
      status: "ACTIVE"
    },
    {
      id: 3,
      name: "自習室監督",
      description: "自習室の監督業務",
      requiresStudent: false,
      status: "ACTIVE"
    },
    {
      id: 4,
      name: "事務作業",
      description: "教材作成や事務処理",
      requiresStudent: false,
      status: "ACTIVE"
    }
  ]
};
