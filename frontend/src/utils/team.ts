export const getTeamStatusText = (
  status: number
): string => {

  switch (status) {

    case 0:
      return '公开';

    case 1:
      return '私有';

    case 2:
      return '加密';

    default:
      return '未知';

  }
};
export const getTeamStatusType = (
  status: number
): 'primary' | 'success' | 'danger' | 'warning' | 'default' => {

  switch (status) {

    case 0:
      return 'primary';

    case 1:
      return 'default';

    case 2:
      return 'warning';

    default:
      return 'default';

  }

};