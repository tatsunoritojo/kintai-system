// UI初期化スクリプト

// ページ読み込み時の初期化
document.addEventListener('DOMContentLoaded', function () {
    // ユーザー情報の表示
    updateUserInfo();

    // ロール切り替えボタンのイベント
    const switchRoleBtn = document.getElementById('switchRoleBtn');
    if (switchRoleBtn) {
        switchRoleBtn.addEventListener('click', function (e) {
            e.preventDefault();
            toggleRole();
        });
    }
});

// ユーザー情報の更新
function updateUserInfo() {
    const userNameElements = document.querySelectorAll('.user-name');
    userNameElements.forEach(el => {
        el.textContent = mockData.currentUser.name;
    });

    const userRoleElements = document.querySelectorAll('.user-role');
    userRoleElements.forEach(el => {
        el.textContent = mockData.currentUser.role;
    });
}

// ロール切り替え
function toggleRole() {
    if (mockData.currentUser.role === 'ADMIN') {
        mockData.currentUser.role = 'EMPLOYEE';
    } else {
        mockData.currentUser.role = 'ADMIN';
    }

    // ページをリロード
    location.reload();
}

// 数値フォーマット(カンマ区切り)
function formatNumber(num) {
    return num.toLocaleString('ja-JP');
}

// 金額フォーマット
function formatCurrency(amount) {
    return '¥' + formatNumber(amount);
}

// 日付フォーマット
function formatDate(dateStr) {
    const date = new Date(dateStr);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

// 曜日取得
function getDayOfWeek(dateStr) {
    const days = ['日', '月', '火', '水', '木', '金', '土'];
    const date = new Date(dateStr);
    return days[date.getDay()];
}

// ステータスバッジのクラス取得
function getStatusBadgeClass(status) {
    const statusMap = {
        'ACTIVE': 'bg-success',
        'INACTIVE': 'bg-secondary',
        'PAID': 'bg-success',
        'PENDING': 'bg-warning',
        'CANCELLED': 'bg-danger'
    };
    return statusMap[status] || 'bg-secondary';
}

// ステータスラベル取得
function getStatusLabel(status) {
    const labelMap = {
        'ACTIVE': '有効',
        'INACTIVE': '無効',
        'PAID': '支払済',
        'PENDING': '未払い',
        'CANCELLED': 'キャンセル'
    };
    return labelMap[status] || status;
}

// 通知アイコン取得
function getNotificationIcon(type) {
    const iconMap = {
        'info': 'fa-info-circle',
        'success': 'fa-check-circle',
        'warning': 'fa-exclamation-triangle',
        'danger': 'fa-exclamation-circle'
    };
    return iconMap[type] || 'fa-info-circle';
}

// テーブル行クリックイベント
function addTableRowClickEvent(tableId, detailPageUrl) {
    const table = document.getElementById(tableId);
    if (!table) return;

    const rows = table.querySelectorAll('tbody tr');
    rows.forEach((row, index) => {
        row.style.cursor = 'pointer';
        row.addEventListener('click', function () {
            const id = this.dataset.id || (index + 1);
            window.location.href = detailPageUrl.replace('{id}', id);
        });
    });
}

// 削除確認ダイアログ
function confirmDelete(itemName) {
    return confirm(`「${itemName}」を削除してもよろしいですか?\nこの操作は取り消せません。`);
}

// 成功メッセージ表示
function showSuccessMessage(message) {
    alert('✓ ' + message);
}

// エラーメッセージ表示
function showErrorMessage(message) {
    alert('✗ ' + message);
}
