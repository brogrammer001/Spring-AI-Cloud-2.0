$raw = Get-Content 'E:\myProject\SpringAi-Cloud\mall-ui-Vue3\src\assets\styles\tailwind.scss' -Raw
$classes = @('.bg-blue-500','.bg-indigo-600','.bg-rose-500','.bg-pink-500','.bg-violet-500','.bg-purple-500','.bg-emerald-50','.bg-fuchsia-500','.rounded-2xl','.rounded-3xl','.shadow-sm','.bg-white','.bg-gray-50','.text-gray-400','.bg-blue-100','.text-blue-700','.bg-indigo-50','.text-indigo-700','.bg-emerald-900\/40')
foreach ($c in $classes) {
  if ($raw -match [regex]::Escape($c)) { Write-Output ("HAS " + $c) } else { Write-Output ("NO  " + $c) }
}
